# ============================================================================
# Spring Boot + Pekko Actors Stock Service - Simplified Makefile
# ============================================================================

# Configuration
DOCKER_IMAGE := stocks-service
CONTAINER_RUNTIME := podman
COMPOSE_CMD := $(shell if command -v podman-compose >/dev/null 2>&1; then echo podman-compose; else echo "podman compose"; fi)

# Colors
BLUE := \033[36m
GREEN := \033[32m
YELLOW := \033[33m
RED := \033[31m
RESET := \033[0m

.DEFAULT_GOAL := help

# ============================================================================
# Help
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
	@echo "  make run         # Run application locally"
	@echo "  make full        # Everything in containers"

# ============================================================================
# Development
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
dev: ## Start monitoring stack for local development
	@echo "$(BLUE)Starting development environment...$(RESET)"
	@echo "$(YELLOW)📊 Starting monitoring stack...$(RESET)"
	$(COMPOSE_CMD) --profile monitoring up -d
	@echo ""
	@echo "$(GREEN)✅ Development environment ready!$(RESET)"
	@echo "$(YELLOW)📝 Run your application:$(RESET) make run"
	@echo "$(GREEN)📊 Monitoring:$(RESET)"
	@echo "  • Grafana: http://localhost:3000 (admin/admin)"
	@echo "  • Prometheus: http://localhost:9090"
	@echo "  • Jaeger: http://localhost:16686"
	@echo ""
	@echo "$(YELLOW)💡 For container metrics, run:$(RESET) make dev-with-cadvisor"

.PHONY: dev-with-cadvisor
dev-with-cadvisor: ## Start monitoring stack with container metrics
	@echo "$(BLUE)Starting development environment with cAdvisor...$(RESET)"
	$(COMPOSE_CMD) --profile monitoring --profile cadvisor up -d
	@echo ""
	@echo "$(GREEN)✅ Development environment with cAdvisor ready!$(RESET)"
	@echo "$(YELLOW)📝 Run your application:$(RESET) make run"
	@echo "$(GREEN)📊 Monitoring:$(RESET)"
	@echo "  • Grafana: http://localhost:3000 (admin/admin)"
	@echo "  • Prometheus: http://localhost:9090"
	@echo "  • Jaeger: http://localhost:16686"
	@echo "  • cAdvisor: http://localhost:8082"

.PHONY: full
full: ## Start everything in containers
	@echo "$(BLUE)Starting full containerized stack...$(RESET)"
	$(COMPOSE_CMD) --profile full up -d --build
	@echo ""
	@echo "$(GREEN)✅ Full stack ready!$(RESET)"
	@echo "$(GREEN)🚀 Application:$(RESET) http://localhost:8080"
	@echo "$(GREEN)🏥 Health Check:$(RESET) http://localhost:8081/actuator/health"
	@echo "$(GREEN)📊 Monitoring:$(RESET)"
	@echo "  • Grafana: http://localhost:3000 (admin/admin)"
	@echo "  • Prometheus: http://localhost:9090"
	@echo "  • Jaeger: http://localhost:16686"

.PHONY: full-with-cadvisor
full-with-cadvisor: ## Start everything with container metrics
	@echo "$(BLUE)Starting full stack with cAdvisor...$(RESET)"
	$(COMPOSE_CMD) --profile full --profile cadvisor up -d --build
	@echo ""
	@echo "$(GREEN)✅ Full stack with cAdvisor ready!$(RESET)"
	@echo "$(GREEN)🚀 Application:$(RESET) http://localhost:8080"
	@echo "$(GREEN)📊 Monitoring:$(RESET)"
	@echo "  • Grafana: http://localhost:3000 (admin/admin)"
	@echo "  • Prometheus: http://localhost:9090"
	@echo "  • Jaeger: http://localhost:16686"
	@echo "  • cAdvisor: http://localhost:8082"

# ============================================================================
# Testing
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

# ============================================================================
# Management
# ============================================================================
.PHONY: stop
stop: ## Stop all services
	@echo "$(BLUE)Stopping all services...$(RESET)"
	$(COMPOSE_CMD) --profile full --profile monitoring --profile cadvisor down

.PHONY: clean
clean: ## Clean build artifacts and containers
	@echo "$(BLUE)Cleaning up...$(RESET)"
	./mvnw clean
	$(COMPOSE_CMD) --profile full --profile monitoring --profile cadvisor down -v --remove-orphans
	$(CONTAINER_RUNTIME) system prune -f
	@echo "$(GREEN)✅ Cleanup complete!$(RESET)"

# ============================================================================
# Monitoring & Debugging
# ============================================================================
.PHONY: logs
logs: ## Show application logs
	@echo "$(BLUE)Application logs:$(RESET)"
	@$(COMPOSE_CMD) logs -f stocks-service 2>/dev/null || \
	 tail -f logs/application.log 2>/dev/null || \
	 echo "$(YELLOW)No logs available$(RESET)"

.PHONY: health
health: ## Check application health
	@echo "$(BLUE)Health check:$(RESET)"
	@curl -s http://localhost:8081/actuator/health | jq . 2>/dev/null || echo "$(RED)Application not running$(RESET)"

.PHONY: verify
verify: ## Verify observability stack
	@echo "$(BLUE)Verifying observability stack...$(RESET)"
	@./scripts/verify-observability.sh

# ============================================================================
# Utilities
# ============================================================================
.PHONY: status
status: ## Show status of all services
	@echo "$(BLUE)Service Status:$(RESET)"
	@$(COMPOSE_CMD) ps 2>/dev/null || echo "No containers running"
	@curl -s -f http://localhost:8081/actuator/health >/dev/null && echo "✅ Application healthy" || echo "❌ Application not running"

.PHONY: urls
urls: ## Show important service URLs
	@echo "$(BLUE)🔗 Service URLs:$(RESET)"
	@echo "$(GREEN)📱 Application:$(RESET)"
	@echo "  http://localhost:8080          - Main API"
	@echo "  http://localhost:8081/actuator - Management endpoints"
	@echo "$(GREEN)📊 Monitoring:$(RESET)"
	@echo "  http://localhost:3000          - Grafana (admin/admin)"
	@echo "  http://localhost:9090          - Prometheus"
	@echo "  http://localhost:16686         - Jaeger tracing"
	@echo "  http://localhost:8082          - cAdvisor (if enabled)"

# ============================================================================
# Aliases (for backward compatibility)
# ============================================================================
.PHONY: start
start: dev ## Alias for 'dev'

.PHONY: monitor
monitor: dev ## Alias for 'dev'

.PHONY: observability
observability: dev ## Alias for 'dev'

.PHONY: observability-with-cadvisor
observability-with-cadvisor: dev-with-cadvisor ## Alias for 'dev-with-cadvisor'

.PHONY: full-stack
full-stack: full ## Alias for 'full'
