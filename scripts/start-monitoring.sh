#!/bin/bash

# Stocks Application - Complete Monitoring Stack Startup Script
# This script starts the complete observability stack including:
# - Application with OpenTelemetry
# - Prometheus (metrics)
# - Grafana (dashboards)
# - Jaeger (tracing)
# - Alertmanager (alerts)
# - Loki (logs)
# - Promtail (log collection)
# - cAdvisor & Node Exporter (system metrics)

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
check_docker() {
    print_status "Checking Docker..."
    if ! docker info >/dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker first."
        exit 1
    fi
    print_success "Docker is running"
}

# Check if Docker Compose is available
check_docker_compose() {
    print_status "Checking Docker Compose..."
    if ! command -v docker-compose >/dev/null 2>&1; then
        print_error "Docker Compose is not installed. Please install Docker Compose."
        exit 1
    fi
    print_success "Docker Compose is available"
}

# Create necessary directories
create_directories() {
    print_status "Creating necessary directories..."

    # Create monitoring directories
    mkdir -p monitoring/grafana/dashboards/{application,infrastructure,observability}
    mkdir -p monitoring/rules
    mkdir -p logs

    # Set proper permissions
    chmod -R 755 monitoring/
    chmod -R 777 logs/

    print_success "Directories created and permissions set"
}

# Validate configuration files
validate_configs() {
    print_status "Validating configuration files..."

    # Check if essential config files exist
    configs=(
        "monitoring/prometheus.yml"
        "monitoring/alertmanager.yml"
        "monitoring/loki.yml"
        "monitoring/promtail.yml"
        "monitoring/otel-collector.yml"
        "monitoring/grafana/provisioning/datasources/datasources.yml"
        "monitoring/grafana/provisioning/dashboards/dashboards.yml"
    )

    for config in "${configs[@]}"; do
        if [[ ! -f "$config" ]]; then
            print_error "Configuration file $config is missing!"
            exit 1
        fi
    done

    print_success "All configuration files are present"
}

# Stop existing containers
stop_existing() {
    print_status "Stopping any existing containers..."
    docker-compose down --remove-orphans || true
    print_success "Existing containers stopped"
}

# Start the monitoring stack
start_monitoring_stack() {
    print_status "Starting the complete monitoring stack..."

    # Start infrastructure services first
    print_status "Starting infrastructure services..."
    docker-compose up -d redis postgres

    # Wait a bit for databases to be ready
    sleep 10

    # Start monitoring services
    print_status "Starting monitoring services..."
    docker-compose up -d prometheus loki alertmanager node-exporter cadvisor

    # Wait for monitoring services to be ready
    sleep 15

    # Start OpenTelemetry and Jaeger
    print_status "Starting tracing services..."
    docker-compose up -d otel-collector jaeger

    # Wait for tracing services
    sleep 10

    # Start log collection
    print_status "Starting log collection services..."
    docker-compose up -d promtail

    # Start Grafana
    print_status "Starting Grafana..."
    docker-compose up -d grafana

    # Finally start the application
    print_status "Starting the Stocks application..."
    docker-compose up -d stocks-service

    # Start optional services
    print_status "Starting optional services..."
    docker-compose up -d wiremock

    print_success "All services started successfully!"
}

# Check service health
check_health() {
    print_status "Checking service health..."

    # Give services time to start
    sleep 30

    services=(
        "http://localhost:8080/actuator/health:Stocks Application"
        "http://localhost:9090/-/healthy:Prometheus"
        "http://localhost:3100/ready:Loki"
        "http://localhost:3000/api/health:Grafana"
        "http://localhost:16686/health:Jaeger"
        "http://localhost:9093/-/healthy:Alertmanager"
    )

    all_healthy=true

    for service in "${services[@]}"; do
        url="${service%%:*}"
        name="${service##*:}"

        if curl -s -f "$url" >/dev/null 2>&1; then
            print_success "$name is healthy"
        else
            print_warning "$name is not responding (this may be normal during startup)"
            all_healthy=false
        fi
    done

    if $all_healthy; then
        print_success "All services are healthy!"
    else
        print_warning "Some services are still starting up. Please wait a few minutes and check again."
    fi
}

# Display access information
show_access_info() {
    echo
    echo "============================================="
    echo "🚀 STOCKS MONITORING STACK IS READY! 🚀"
    echo "============================================="
    echo
    echo "📊 SERVICE ENDPOINTS:"
    echo "  • Stocks Application:    http://localhost:8080"
    echo "  • Stocks API Docs:       http://localhost:8080/actuator"
    echo "  • Grafana Dashboards:    http://localhost:3000 (admin/admin)"
    echo "  • Prometheus Metrics:    http://localhost:9090"
    echo "  • Jaeger Tracing:        http://localhost:16686"
    echo "  • Alertmanager:          http://localhost:9093"
    echo "  • Loki Logs:             http://localhost:3100"
    echo
    echo "🔧 MONITORING ENDPOINTS:"
    echo "  • cAdvisor:              http://localhost:8082"
    echo "  • Node Exporter:         http://localhost:9100"
    echo "  • OTEL Collector:        http://localhost:8888"
    echo
    echo "🧪 TESTING ENDPOINTS:"
    echo "  • WireMock:              http://localhost:8089"
    echo
    echo "📋 SAMPLE API CALLS:"
    echo "  curl http://localhost:8080/stocks"
    echo "  curl http://localhost:8080/stocks/AAPL"
    echo
    echo "📖 DOCUMENTATION:"
    echo "  • Observability Guide:   docs/OBSERVABILITY.md"
    echo
    echo "To stop all services: docker-compose down"
    echo "To view logs: docker-compose logs -f [service-name]"
    echo "============================================="
}

# Main execution
main() {
    echo "🚀 Starting Complete Stocks Monitoring Stack..."
    echo

    check_docker
    check_docker_compose
    create_directories
    validate_configs
    stop_existing
    start_monitoring_stack
    check_health
    show_access_info

    print_success "Monitoring stack startup complete!"
}

# Handle script interruption
trap 'print_error "Script interrupted"; exit 1' INT TERM

# Run main function
main "$@"
