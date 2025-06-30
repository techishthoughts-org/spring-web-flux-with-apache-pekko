# ============================================================================
# Spring Boot + Pekko Actors Stock Service - Simplified Makefile
# ============================================================================

# Project Configuration
DOCKER_IMAGE := stocks-service
CONTAINER_RUNTIME := podman
COMPOSE_CMD := $(shell if command -v podman-compose >/dev/null 2>&1; then echo podman-compose; else echo "podman compose"; fi)

# Colors for output
BLUE := \033[36m
GREEN := \033[32m
YELLOW := \033[33m
RED := \033[31m
RESET := \033[0m

# Default target
.DEFAULT_GOAL := help

# ============================================================================
# Help Target
# ============================================================================
.PHONY: help
help: ## Show available commands
	@echo "$(BLUE)🚀 Spring Boot + Pekko Stocks Service$(RESET)"
	@echo "$(BLUE)====================================$(RESET)"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(YELLOW)%-20s$(RESET) %s\n", $$1, $$2}'
	@echo ""
	@echo "$(BLUE)Quick Start:$(RESET)"
	@echo "  make dev         # Start development environment"
	@echo "  make test        # Run all tests"
	@echo "  make observability # Start with monitoring stack"

# ============================================================================
# Development Commands
# ============================================================================
.PHONY: build
build: ## Build the application
	@echo "$(BLUE)Building application...$(RESET)"
	./mvnw clean compile -DskipTests

.PHONY: run
run: ## Run the application locally
	@echo "$(BLUE)Starting application...$(RESET)"
	./mvnw spring-boot:run

.PHONY: dev
dev: ## Start complete development environment
	@echo "$(BLUE)Starting development environment...$(RESET)"
	$(COMPOSE_CMD) --profile default up -d
	@echo "$(GREEN)✅ Development environment ready!$(RESET)"
	@echo ""
	@echo "$(YELLOW)📱 Application:$(RESET)  http://localhost:8080"
	@echo "$(YELLOW)🏥 Health:$(RESET)       http://localhost:8081/actuator/health"
	@echo "$(YELLOW)📊 Metrics:$(RESET)      http://localhost:8081/actuator/prometheus"

.PHONY: observability
observability: ## Start application with full observability stack
	@echo "$(BLUE)Starting observability stack...$(RESET)"
	$(COMPOSE_CMD) up -d stocks-service prometheus loki jaeger otel-collector grafana alertmanager promtail node-exporter cadvisor pyroscope
	@echo "$(GREEN)✅ Observability stack ready!$(RESET)"
	@echo ""
	@echo "$(GREEN)📊 Monitoring URLs:$(RESET)"
	@echo "  • Application:  http://localhost:8080"
	@echo "  • Grafana:      http://localhost:3000 (admin/admin)"
	@echo "  • Prometheus:   http://localhost:9090"
	@echo "  • Jaeger:       http://localhost:16686"

.PHONY: stop
stop: ## Stop all services
	@echo "$(BLUE)Stopping all services...$(RESET)"
	$(COMPOSE_CMD) down

.PHONY: clean
clean: ## Clean build artifacts and containers
	@echo "$(BLUE)Cleaning up...$(RESET)"
	./mvnw clean
	$(COMPOSE_CMD) down -v --remove-orphans
	$(CONTAINER_RUNTIME) system prune -f
	@echo "$(GREEN)✅ Cleanup complete!$(RESET)"

# ============================================================================
# Testing Commands
# ============================================================================
.PHONY: test
test: ## Run all tests
	@echo "$(BLUE)Running all tests...$(RESET)"
	./mvnw test

.PHONY: test-unit
test-unit: ## Run unit tests only
	@echo "$(BLUE)Running unit tests...$(RESET)"
	./mvnw test -Dtest="**/unit/**"

.PHONY: test-integration
test-integration: ## Run integration tests only
	@echo "$(BLUE)Running integration tests...$(RESET)"
	./mvnw test -Dtest="**/integration/**"

.PHONY: test-component
test-component: ## Run component tests (BDD)
	@echo "$(BLUE)Running component tests...$(RESET)"
	./mvnw test -Dtest="**/component/**"

.PHONY: test-performance
test-performance: ## Run performance tests
	@echo "$(BLUE)Running performance tests...$(RESET)"
	./mvnw test -Pperformance

.PHONY: verify
verify: ## Run all tests and quality checks
	@echo "$(BLUE)Running verification...$(RESET)"
	./mvnw clean verify

# ============================================================================
# Docker Commands
# ============================================================================
.PHONY: build-docker
build-docker: ## Build Docker image
	@echo "$(BLUE)Building Docker image...$(RESET)"
	$(CONTAINER_RUNTIME) build -t $(DOCKER_IMAGE):latest .

.PHONY: run-docker
run-docker: ## Run application in Docker
	@echo "$(BLUE)Running in Docker...$(RESET)"
	$(COMPOSE_CMD) up -d stocks-service

# ============================================================================
# Monitoring & Debugging
# ============================================================================
.PHONY: logs
logs: ## Show application logs
	@echo "$(BLUE)Application logs:$(RESET)"
	$(COMPOSE_CMD) logs -f stocks-service 2>/dev/null || tail -f logs/application.log 2>/dev/null || echo "$(YELLOW)No logs available$(RESET)"

.PHONY: health
health: ## Check application health
	@echo "$(BLUE)Health check:$(RESET)"
	@curl -s http://localhost:8081/actuator/health | jq . 2>/dev/null || echo "$(RED)Application not running$(RESET)"

.PHONY: taskmaster
taskmaster: ## Check taskmaster (Pekko actor system) status
	@echo "$(BLUE)Taskmaster status:$(RESET)"
	@curl -s http://localhost:8081/actuator/health/stockActor | jq . 2>/dev/null || echo "$(RED)Taskmaster not available$(RESET)"

.PHONY: status
status: ## Show status of all services
	@echo "$(BLUE)Service Status:$(RESET)"
	@echo "$(YELLOW)Containers:$(RESET)"
	@$(COMPOSE_CMD) ps 2>/dev/null || echo "No containers running"
	@echo "$(YELLOW)Application:$(RESET)"
	@curl -s -f http://localhost:8081/actuator/health >/dev/null && echo "✅ Application healthy" || echo "❌ Application not running"

# ============================================================================
# API Testing
# ============================================================================
.PHONY: api-test
api-test: ## Test API endpoints
	@echo "$(BLUE)Testing API endpoints...$(RESET)"
	@curl -s http://localhost:8080/stocks | jq . || echo "$(RED)API not available$(RESET)"

# ============================================================================
# Documentation & Setup
# ============================================================================
.PHONY: setup
setup: ## Initial project setup
	@echo "$(BLUE)Setting up project...$(RESET)"
	chmod +x mvnw scripts/*.sh
	npm install 2>/dev/null || echo "$(YELLOW)npm not available, skipping$(RESET)"
	@echo "$(GREEN)✅ Setup complete!$(RESET)"

.PHONY: docs-check
docs-check: ## Check documentation and README formatting
	@echo "$(BLUE)Checking documentation and README...$(RESET)"
	npm run docs:lint || echo "$(RED)npm or markdownlint not available$(RESET)"

.PHONY: docs-fix
docs-fix: ## Fix documentation and README formatting
	@echo "$(BLUE)Fixing documentation and README...$(RESET)"
	npm run docs:fix || echo "$(RED)npm or markdownlint not available$(RESET)"

# ============================================================================
# Utility Commands
# ============================================================================
.PHONY: version
version: ## Show project version
	@./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout

.PHONY: env-check
env-check: ## Check development environment
	@echo "$(BLUE)Environment Check:$(RESET)"
	@echo -n "☕ Java: "; java -version 2>&1 | head -1 || echo "❌ Not found"
	@echo -n "📦 Maven: "; ./mvnw --version 2>&1 | head -1 || echo "❌ Not working"
	@echo -n "🐳 Container: "; $(CONTAINER_RUNTIME) --version || echo "❌ Not found"
	@echo -n "🔧 Compose: "; $(COMPOSE_CMD) --version || echo "❌ Not found"
	@echo -n "📋 jq: "; jq --version 2>/dev/null || echo "⚠️  Optional tool missing"
	@echo -n "🌐 curl: "; curl --version 2>&1 | head -1 || echo "❌ Not found"

.PHONY: urls
urls: ## Show important service URLs
	@echo "$(BLUE)🔗 Service URLs:$(RESET)"
	@echo ""
	@echo "$(GREEN)📱 Application:$(RESET)"
	@echo "  http://localhost:8080          - Main API"
	@echo "  http://localhost:8081/actuator - Management endpoints"
	@echo ""
	@echo "$(GREEN)📊 Monitoring:$(RESET)"
	@echo "  http://localhost:3000          - Grafana (admin/admin)"
	@echo "  http://localhost:9090          - Prometheus"
	@echo "  http://localhost:16686         - Jaeger tracing"
	@echo ""
	@echo "$(GREEN)🔍 Debugging:$(RESET)"
	@echo "  http://localhost:4040          - Pyroscope profiling"
	@echo "  http://localhost:8082          - cAdvisor"

# ============================================================================
# Aliases for common workflows
# ============================================================================
.PHONY: start
start: dev ## Alias for 'dev' - start development environment

.PHONY: monitor
monitor: observability ## Alias for 'observability' - start with monitoring

.PHONY: check
check: verify ## Alias for 'verify' - run all checks
