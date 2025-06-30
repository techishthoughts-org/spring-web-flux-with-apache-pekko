# 🐳 Docker Configuration Guide

This guide explains how to build, run, and deploy the Spring Boot + Pekko Actors
Stock Service using Docker.

## 📋 Table of Contents

- [Quick Start](#-quick-start)
- [Building the Docker Image](#️-building-the-docker-image)
- [Running with Docker Compose](#-running-with-docker-compose)
- [Environment Configuration](#️-environment-configuration)
- [Production Deployment](#-production-deployment)
- [Monitoring & Observability](#-monitoring--observability)
- [Troubleshooting](#-troubleshooting)

## 🚀 Quick Start

### Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- 4GB+ available RAM
- API keys for external stock services

### 1. Clone and Setup

```bash

# Clone the repository

git clone https://github.com/techishthoughts-org/spring-web-flux-with-apache-pekko.git
cd stocks-demo

# Copy environment template

cp env.example .env

# Edit .env file with your API keys

vim .env

```text

### 2. Build and Run

```bash

# Build and start all services

docker-compose up --build

# Or run in background

docker-compose up -d --build

```text

### 3. Verify Services

```bash

# Check application health

curl <http://localhost:8081/actuator/health>

# Test stock endpoint

curl <http://localhost:8080/stocks>

# Access monitoring dashboards

open <http://localhost:3000>  # Grafana (admin/admin)
open <http://localhost:9090>  # Prometheus

```text

## 🏗️ Building the Docker Image

### Multi-Stage Build Process

Our Dockerfile uses a multi-stage build for optimization:

1. **Builder Stage**: Compiles the application using Maven
1. **Runtime Stage**: Creates optimized production image

### Build Commands

```bash

# Build the application image

docker build -t stocks-service:latest .

# Build specific stage

docker build --target builder -t stocks-service:builder .
docker build --target runtime -t stocks-service:runtime .

# Build with build arguments

docker build \
  --build-arg MAVEN_OPTS="-Xmx2g" \
  --build-arg SKIP_TESTS=true \
  -t stocks-service:latest .

```text

### Image Optimization Features

- **Layer Caching**: Dependencies downloaded separately for better caching
- **Non-root User**: Runs as `appuser` for security
- **Health Checks**: Built-in health monitoring
- **Security Updates**: Latest security patches included
- **Resource Limits**: Optimized JVM settings for containers

## 🐳 Running with Docker Compose

### Available Services

| Service | Port | Description |
|---------|------|-------------|
| stocks-service | 8080 | Main application |
| stocks-service | 8081 | Management/Actuator |
| redis | 6379 | Cache storage |
| wiremock | 8089 | API mocking for tests |
| prometheus | 9090 | Metrics collection |
| grafana | 3000 | Monitoring dashboard |
| postgres | 5432 | Database (optional) |

### Run Configurations

#### Development Mode

```bash

# Run core services only

docker-compose up stocks-service redis wiremock

# Run with hot reload (if configured)

docker-compose -f docker-compose.yml -f docker-compose.dev.yml up

```text

#### Full Stack with Monitoring

```bash

# Run all services including monitoring

docker-compose --profile monitoring up -d

# View logs

docker-compose logs -f stocks-service

```text

#### With Database

```bash

# Run with PostgreSQL database

docker-compose --profile database up -d

```text

### Docker Compose Commands

```bash

# Build and start

docker-compose up --build

# Start in background

docker-compose up -d

# Stop all services

docker-compose down

# Stop and remove volumes

docker-compose down -v

# View logs

docker-compose logs -f [service-name]

# Scale service

docker-compose up -d --scale stocks-service=3

# Check service status

docker-compose ps

```text

## ⚙️ Environment Configuration

### Required Environment Variables

Create a `.env` file from `env.example`:

```bash

# External API Keys (Required)

FINNHUB_API_KEY=your-key-here
ALPHA_VANTAGE_KEY=your-key-here
TWELVEDATA_API_KEY=your-key-here

# Database (Optional)

POSTGRES_PASSWORD=secure-password
REDIS_PASSWORD=redis-password

# Monitoring

GRAFANA_PASSWORD=admin-password

```text

### Spring Profiles

The application supports multiple Spring profiles:

- `docker`: Optimized for container environments
- `local`: Local development settings
- `test`: Testing configuration
- `prod`: Production settings

### JVM Configuration

Optimized JVM settings for containers:

```bash
JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:G1HeapRegionSize=16m \
  -XX:+UseStringDeduplication"

```text

## 🚀 Production Deployment

### Security Considerations

1. **Use Non-root User**: Image runs as `appuser:appgroup`
1. **Resource Limits**: Set appropriate memory/CPU limits
1. **Health Checks**: Configured for load balancer integration
1. **Secrets Management**: Use external secret management
1. **Network Security**: Use custom networks and firewalls

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: stocks-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: stocks-service
  template:
    metadata:
      labels:
        app: stocks-service
    spec:
      containers:

      - name: stocks-service
        image: stocks-service:latest
        ports:

        - containerPort: 8080
        - containerPort: 8081
        env:

        - name: SPRING_PROFILES_ACTIVE
          value: "prod,k8s"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "2Gi"
            cpu: "1"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10

```text

### Production Environment Variables

```bash

# Production settings

SPRING_PROFILES_ACTIVE=prod
JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Security

MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus

# Monitoring

MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED=true

```text

## 📊 Monitoring & Observability

### Grafana Dashboards

Access Grafana at `<http://localhost:3000`:>

- Username: `admin`
- Password: Set in `GRAFANA_PASSWORD` env var

**Available Dashboards**:

- Application Metrics
- JVM Performance
- Pekko Actor System
- HTTP Request Metrics
- Custom Business Metrics

### Prometheus Metrics

Access Prometheus at `<http://localhost:9090`:>

**Key Metrics**:

- `http_server_requests_seconds_*`: HTTP request metrics
- `jvm_*`: JVM performance metrics
- `pekko_*`: Actor system metrics
- `stocks_*`: Custom application metrics

### Health Checks

Multiple health check endpoints:

```bash

# Overall health

curl <http://localhost:8081/actuator/health>

# Detailed health

curl <http://localhost:8081/actuator/health/details>

# Readiness probe

curl <http://localhost:8081/actuator/health/readiness>

# Liveness probe

curl <http://localhost:8081/actuator/health/liveness>

```text

### Logging

Structured JSON logging is configured:

```bash

# View application logs

docker-compose logs -f stocks-service

# View logs with specific level

docker-compose logs stocks-service | grep ERROR

# Export logs

docker-compose logs --no-color stocks-service > app.log

```text

## 🔧 Troubleshooting

### Common Issues

#### 1. Application Won't Start

```bash

# Check logs

docker-compose logs stocks-service

# Check Java version

docker run stocks-service:latest java -version

# Verify memory settings

docker stats stocks-service

```text

#### 2. API Keys Not Working

```bash

# Verify environment variables

docker-compose exec stocks-service env | grep API_KEY

# Test API connectivity

docker-compose exec stocks-service curl -v
<https://finnhub.io/api/v1/quote?symbol=AAPL&token=$FINNHUB_API_KEY>

```text

#### 3. Database Connection Issues

```bash

# Check PostgreSQL status

docker-compose ps postgres

# Test connection

docker-compose exec postgres psql -U stocks_user -d stocks_db -c "SELECT 1;"

# Check Redis connection

docker-compose exec redis redis-cli ping

```text

#### 4. Memory Issues

```bash

# Check memory usage

docker stats

# Adjust JVM settings

export JAVA_OPTS="-XX:MaxRAMPercentage=50.0"
docker-compose up -d

```text

### Debug Commands

```bash

# Enter running container

docker-compose exec stocks-service sh

# Check application properties

docker-compose exec stocks-service cat /app/application.properties

# View JVM flags

docker-compose exec stocks-service jinfo -flags 1

# Get thread dump

docker-compose exec stocks-service jstack 1 > threaddump.txt

# Get heap dump

docker-compose exec stocks-service jcmd 1 GC.run_finalization

```text

### Performance Tuning

#### JVM Tuning

```bash

# For development

JAVA_OPTS="-XX:+UseG1GC -Xms512m -Xmx1g"

# For production

JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

```text

#### Container Resource Limits

```yaml
services:
  stocks-service:
    deploy:
      resources:
        limits:
          memory: 2G
          cpus: '1.0'
        reservations:
          memory: 512M
          cpus: '0.5'

```text

## 📝 Additional Resources

- [Spring Boot Docker Documentation](<https://spring.io/guides/spring-boot-docker/>)
- [Pekko Configuration](<https://pekko.apache.org/docs/pekko/current/>)
- [Docker Best Practices](<https://docs.docker.com/develop/dev-best-practices/>)
- [Kubernetes Deployment Guide](./docs/DEPLOYMENT.md)

## 🤝 Contributing

When making changes to Docker configuration:

1. Test locally with `docker-compose up --build`
1. Verify all health checks pass
1. Test with different environment configurations
1. Update this documentation if needed
1. Run security scans: `docker scout cves stocks-service:latest`

---

**💡 Need Help?** Check the [troubleshooting section](#-troubleshooting) or
create an issue in the repository.
