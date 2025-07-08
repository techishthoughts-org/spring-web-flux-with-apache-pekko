#!/bin/bash

set +e

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=== Observability Stack Verification ===${NC}"

# Function to check service
check_service() {
    local service_name=$1
    local url=$2

    echo -n "Checking $service_name... "

    if curl -s -o /dev/null -w "%{http_code}" "$url" | grep -q "200\|302"; then
        echo -e "${GREEN}✓ OK${NC}"
        return 0
    else
        echo -e "${RED}✗ FAILED${NC}"
        return 1
    fi
}

# Function to check metrics
check_metrics() {
    local service_name=$1
    local url=$2
    local metric_name=$3

    echo -n "Checking $service_name metrics... "

    local count=$(curl -s "$url" | grep -c "$metric_name" || echo "0")
    if [ "$count" -gt 0 ]; then
        echo -e "${GREEN}✓ $count metrics found${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠ No metrics found${NC}"
        return 1
    fi
}

echo -e "\n${BLUE}=== Core Services ===${NC}"
check_service "Application Health" "http://localhost:8081/actuator/health"
check_service "Prometheus Endpoint" "http://localhost:8081/actuator/prometheus"

echo -e "\n${BLUE}=== Observability Stack ===${NC}"
check_service "Prometheus" "http://localhost:9090"
check_service "Grafana" "http://localhost:3000"
check_service "Jaeger" "http://localhost:16686"
check_service "Loki" "http://localhost:3100"

echo -e "\n${BLUE}=== Metrics Collection ===${NC}"
check_metrics "Application" "http://localhost:8081/actuator/prometheus" "jvm_memory_used_bytes"
check_metrics "Prometheus" "http://localhost:9090/api/v1/query?query=up" "up"

echo -e "\n${BLUE}=== Prometheus Targets ===${NC}"
echo "Active Targets:"
curl -s "http://localhost:9090/api/v1/targets" | jq -r '.data.activeTargets[] | "  \(.labels.job): \(.health)"' 2>/dev/null || echo "  Unable to fetch targets"

echo -e "\n${GREEN}🎉 Verification complete!${NC}"
echo -e "${BLUE}Access points:${NC}"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo "  - Prometheus: http://localhost:9090"
echo "  - Jaeger: http://localhost:16686"
echo "  - Application: http://localhost:8080"
