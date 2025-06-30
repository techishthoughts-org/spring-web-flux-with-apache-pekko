#!/bin/bash

# Act Helper Script for Spring Boot + Pekko Stocks Service
# This script provides easy commands to test GitHub Actions locally

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}🚀 $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check if Act is installed
check_act() {
    if ! command -v act &> /dev/null; then
        print_error "Act is not installed!"
        echo "Install it with: curl -s https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash"
        exit 1
    fi
    print_success "Act is installed: $(act --version)"
}

# Check if Docker is running
check_docker() {
    if ! docker info &> /dev/null; then
        print_error "Docker is not running!"
        echo "Please start Docker and try again."
        exit 1
    fi
    print_success "Docker is running"
}

# Show available workflows and jobs
show_workflows() {
    print_status "Available workflows and jobs:"
    echo ""
    act --list
}

# Run specific job types
run_unit_tests() {
    print_status "Running unit tests locally..."
    act --job unit-tests --secret-file .secrets
}

run_integration_tests() {
    print_status "Running integration tests locally..."
    # Start WireMock container first
    print_status "Starting WireMock container..."
    docker run -d --name wiremock-act -p 8089:8080 wiremock/wiremock:latest || true

    # Run the tests
    act --job integration-tests --secret-file .secrets

    # Clean up
    print_status "Cleaning up WireMock container..."
    docker stop wiremock-act && docker rm wiremock-act || true
}

run_pr_validation() {
    print_status "Running PR validation locally..."
    act pull_request --job fast-validation --secret-file .secrets
}

run_full_ci() {
    print_status "Running full CI pipeline locally..."
    act --workflows .github/workflows/ci.yml --secret-file .secrets
}

run_security_checks() {
    print_status "Running security checks locally..."
    act --job code-quality --secret-file .secrets
}

run_build() {
    print_status "Running build job locally..."
    act --job build --secret-file .secrets
}

# Dry run to see what would execute
dry_run() {
    print_status "Performing dry run of CI workflow..."
    act --workflows .github/workflows/ci.yml --dryrun
}

# Clean up Act containers and images
cleanup() {
    print_status "Cleaning up Act containers and images..."

    # Stop and remove Act containers
    docker ps -a --filter "label=act" --format "{{.ID}}" | xargs -r docker rm -f

    # Remove Act images (optional)
    read -p "Remove Act runner images? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker images --filter "reference=catthehacker/ubuntu" --format "{{.ID}}" | xargs -r docker rmi -f
        print_success "Act images removed"
    fi

    print_success "Cleanup completed"
}

# Show help
show_help() {
    echo "Act Helper Script for Spring Boot + Pekko Stocks Service"
    echo ""
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  list              Show available workflows and jobs"
    echo "  unit              Run unit tests"
    echo "  integration       Run integration tests (with WireMock)"
    echo "  pr                Run PR validation"
    echo "  security          Run security checks"
    echo "  build             Run build job"
    echo "  ci                Run full CI pipeline"
    echo "  dry-run           Show what would execute (dry run)"
    echo "  cleanup           Clean up Act containers and images"
    echo "  help              Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 unit           # Run unit tests locally"
    echo "  $0 pr             # Test PR validation"
    echo "  $0 ci             # Run full CI pipeline"
    echo "  $0 dry-run        # See what would run"
    echo ""
}

# Main script logic
main() {
    print_status "Act Helper for Spring Boot + Pekko Stocks Service"
    echo ""

    # Check prerequisites
    check_act
    check_docker
    echo ""

    # Handle commands
    case "${1:-help}" in
        "list")
            show_workflows
            ;;
        "unit")
            run_unit_tests
            ;;
        "integration")
            run_integration_tests
            ;;
        "pr")
            run_pr_validation
            ;;
        "security")
            run_security_checks
            ;;
        "build")
            run_build
            ;;
        "ci")
            run_full_ci
            ;;
        "dry-run")
            dry_run
            ;;
        "cleanup")
            cleanup
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

# Run main function with all arguments
main "$@"
