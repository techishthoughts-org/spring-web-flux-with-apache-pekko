# Deployment Guide

## Overview

This guide covers deployment strategies and configurations for the Stock Service application. The application uses a **single `docker-compose.yml` with profiles** for different deployment scenarios, simplifying the deployment process.

## 🚀 Deployment Modes

### 1. Local Development (Recommended)

Best for active development and debugging:

```bash
# Start monitoring stack for local development
make dev

# Run application locally (in another terminal)
make run
```

**Characteristics:**
- Application runs on host machine with hot reload
- Complete observability stack in containers
- Fast startup and restart
- Easy debugging and profiling
- Full IDE integration

### 2. Development with Container Metrics

Enhanced local development with container monitoring:

```bash
# Start monitoring stack with container metrics
make dev-with-cadvisor

# Run application locally
make run
```

**Characteristics:**
- All monitoring features from local development
- Additional container resource monitoring
- cAdvisor provides container-level metrics

### 3. Full Containerization

Complete containerized deployment:

```bash
# Start full stack in containers
make full
```

**Characteristics:**
- All components in containers
- Production-like environment
- Consistent deployment across environments
- Easy scaling and management

### 4. Full Stack with Container Metrics

Complete containerized deployment with enhanced monitoring:

```bash
# Start full stack with container metrics
make full-with-cadvisor
```

**Characteristics:**
- All benefits of full containerization
- Enhanced container monitoring with cAdvisor
- Comprehensive resource utilization tracking

## 🐳 Docker Deployment

### Single Docker Compose with Profiles

The application uses a **single `docker-compose.yml`** with profiles for different deployment scenarios:

```yaml
# docker-compose.yml
version: '3.8'

networks:
  stocks-network:
    driver: bridge

services:
  # Application service
  stocks-service:
    build: .
    container_name: stocks-service
    profiles: ["app", "full"]
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - FINNHUB_KEY=${FINNHUB_KEY}
      - OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
    volumes:
      - app-logs:/app/logs
    networks:
      - stocks-network
    depends_on:
      - prometheus
      - jaeger
      - loki

  # Monitoring services (profile: monitoring)
  prometheus:
    image: prom/prometheus:latest
    container_name: stocks-prometheus
    profiles: ["monitoring", "full"]
    ports:
      - "9090:9090"
    # ... configuration
```

### Available Profiles

| Profile | Services Included | Use Case |
|---------|------------------|----------|
| `monitoring` | Observability stack only | Local development |
| `app` | Application service only | Testing application container |
| `full` | Everything | Production-like testing |
| `cadvisor` | Container metrics | Enhanced monitoring |

### Building Application Image

```bash
# Build optimized production image
docker build -t stock-service:latest .

# Build with specific tag
docker build -t stock-service:v1.0.0 .

# Multi-platform build
docker buildx build --platform linux/amd64,linux/arm64 -t stock-service:latest .
```

### Image Optimization

The Dockerfile uses multi-stage builds for optimization:

```dockerfile
# Stage 1: Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Manual Docker Compose Commands

If you need to run specific profiles manually:

```bash
# Monitoring stack only
docker-compose --profile monitoring up -d

# Application only
docker-compose --profile app up -d

# Full stack
docker-compose --profile full up -d

# Full stack with container metrics
docker-compose --profile full --profile cadvisor up -d
```

## ☸️ Kubernetes Deployment

### Namespace Configuration

```yaml
# k8s/namespace.yml
apiVersion: v1
kind: Namespace
metadata:
  name: stocks-service
  labels:
    name: stocks-service
```

### ConfigMap for Application Configuration

```yaml
# k8s/configmap.yml
apiVersion: v1
kind: ConfigMap
metadata:
  name: stocks-config
  namespace: stocks-service
data:
  application.yml: |
    spring:
      application:
        name: stocks-service
      profiles:
        active: kubernetes

    management:
      endpoints:
        web:
          exposure:
            include: health,info,prometheus
      endpoint:
        health:
          show-details: always

    otel:
      exporter:
        otlp:
          endpoint: http://otel-collector:4317

    logging:
      level:
        com.techishthoughts.stocks: INFO
      file:
        path: /app/logs/application.log
```

### Secrets Configuration

```yaml
# k8s/secret.yml
apiVersion: v1
kind: Secret
metadata:
  name: stocks-secrets
  namespace: stocks-service
type: Opaque
data:
  finnhub-key: <base64-encoded-key>
```

### Deployment Configuration

```yaml
# k8s/deployment.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: stocks-service
  namespace: stocks-service
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
        image: stock-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "kubernetes"
        - name: FINNHUB_KEY
          valueFrom:
            secretKeyRef:
              name: stocks-secrets
              key: finnhub-key
        - name: JAVA_OPTS
          value: "-Xms2g -Xmx4g -XX:+UseZGC"
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
        - name: logs-volume
          mountPath: /app/logs
      volumes:
      - name: config-volume
        configMap:
          name: stocks-config
      - name: logs-volume
        emptyDir: {}
```

### Service Configuration

```yaml
# k8s/service.yml
apiVersion: v1
kind: Service
metadata:
  name: stocks-service
  namespace: stocks-service
spec:
  selector:
    app: stocks-service
  ports:
  - name: http
    port: 8080
    targetPort: 8080
  type: ClusterIP
```

### Ingress Configuration

```yaml
# k8s/ingress.yml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: stocks-service-ingress
  namespace: stocks-service
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  ingressClassName: nginx
  rules:
  - host: stocks-api.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: stocks-service
            port:
              number: 8080
```

### HorizontalPodAutoscaler

```yaml
# k8s/hpa.yml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: stocks-service-hpa
  namespace: stocks-service
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: stocks-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### ServiceMonitor for Prometheus

```yaml
# k8s/servicemonitor.yml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: stocks-service-monitor
  namespace: stocks-service
spec:
  selector:
    matchLabels:
      app: stocks-service
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
```

## 🔧 Configuration Management

### Environment Variables

#### Common Environment Variables
```bash
# Application
SPRING_PROFILES_ACTIVE=production
FINNHUB_KEY=your_finnhub_api_key

# JVM Settings
JAVA_OPTS=-Xms2g -Xmx4g -XX:+UseZGC

# OpenTelemetry
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
OTEL_RESOURCE_ATTRIBUTES=service.name=stocks-service

# Logging
LOGGING_LEVEL_COM_TECHISHTHOUGHTS_STOCKS=INFO
LOGGING_FILE_PATH=/app/logs/application.log
```

#### Environment-Specific Variables

**Development:**
```bash
SPRING_PROFILES_ACTIVE=development
LOGGING_LEVEL_COM_TECHISHTHOUGHTS_STOCKS=DEBUG
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
```

**Production:**
```bash
SPRING_PROFILES_ACTIVE=production
JAVA_OPTS=-Xms4g -Xmx8g -XX:+UseZGC -XX:+UnlockExperimentalVMOptions
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector.monitoring.svc.cluster.local:4317
```

### Application Profiles

#### Development Profile
```yaml
# application-development.yml
spring:
  codec:
    max-in-memory-size: 10MB

management:
  endpoints:
    web:
      exposure:
        include: "*"

logging:
  level:
    com.techishthoughts.stocks: DEBUG
    org.springframework.web: DEBUG
```

#### Production Profile
```yaml
# application-production.yml
spring:
  codec:
    max-in-memory-size: 50MB

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus

logging:
  level:
    com.techishthoughts.stocks: INFO
    org.springframework.web: WARN
```

## 🚦 Health Checks

### Application Health Endpoints

```bash
# Simple health check
curl http://localhost:8080/health-simple

# Detailed health check
curl http://localhost:8080/actuator/health

# Component-specific health
curl http://localhost:8080/actuator/health/stockActorHealthIndicator
```

### Docker Health Check

```dockerfile
# In Dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/health-simple || exit 1
```

### Kubernetes Health Checks

```yaml
# In deployment.yml
livenessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

## 🔄 Deployment Strategies

### Blue-Green Deployment

```bash
# Deploy to green environment
kubectl apply -f k8s/deployment-green.yml

# Verify green deployment
kubectl get pods -l version=green

# Switch traffic to green
kubectl patch service stocks-service -p '{"spec":{"selector":{"version":"green"}}}'

# Remove blue deployment
kubectl delete deployment stocks-service-blue
```

### Rolling Updates

```bash
# Update image
kubectl set image deployment/stocks-service stocks-service=stock-service:v2.0.0

# Monitor rollout
kubectl rollout status deployment/stocks-service

# Rollback if needed
kubectl rollout undo deployment/stocks-service
```

### Canary Deployment

```yaml
# Canary deployment with Istio
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: stocks-service-canary
spec:
  hosts:
  - stocks-service
  http:
  - match:
    - headers:
        canary:
          exact: "true"
    route:
    - destination:
        host: stocks-service
        subset: v2
  - route:
    - destination:
        host: stocks-service
        subset: v1
      weight: 90
    - destination:
        host: stocks-service
        subset: v2
      weight: 10
```

## 📊 Monitoring in Production

### Prometheus Configuration

```yaml
# prometheus.yml for production
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "rules/*.yml"

scrape_configs:
  - job_name: 'stocks-service'
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
```

### Grafana Dashboard Provisioning

```yaml
# grafana-dashboards.yml
apiVersion: v1
kind: ConfigMap
metadata:
  name: grafana-dashboards
data:
  stocks-dashboard.json: |
    {
      "dashboard": {
        "title": "Stock Service - Production",
        "panels": [
          {
            "title": "Request Rate",
            "targets": [
              {
                "expr": "rate(http_server_requests_seconds_count[5m])"
              }
            ]
          }
        ]
      }
    }
```

## 🔍 Troubleshooting Deployment

### Common Issues

#### Port Conflicts
```bash
# Check what's using port 8080
lsof -i :8080

# Stop conflicting services
make stop

# Clean up containers
make clean
```

#### Container Startup Issues
```bash
# Check container logs
podman logs stocks-service

# Check container health
podman inspect stocks-service | grep -i health

# Debug container
podman exec -it stocks-service /bin/sh
```

#### Resource Constraints
```bash
# Check resource usage
kubectl top pods -n stocks-service

# Check node resources
kubectl describe nodes

# Scale down if needed
kubectl scale deployment stocks-service --replicas=1
```

### Debugging Commands

```bash
# Development
make status          # Check all services
make health          # Check application health
make logs           # View application logs

# Docker
docker-compose ps   # Check container status
docker-compose logs # View container logs

# Kubernetes
kubectl get pods -n stocks-service
kubectl describe pod <pod-name> -n stocks-service
kubectl logs <pod-name> -n stocks-service
```

## 🎯 Best Practices

### Container Optimization
- Use multi-stage builds
- Minimize image layers
- Use specific base image versions
- Run as non-root user
- Set resource limits

### Security
- Use secrets for sensitive data
- Implement proper RBAC
- Enable network policies
- Regular security scanning
- Use distroless images for production

### Monitoring
- Implement comprehensive health checks
- Set up alerts for critical metrics
- Use distributed tracing
- Monitor resource usage
- Track application metrics

### Scaling
- Use horizontal pod autoscaling
- Implement circuit breakers
- Use connection pooling
- Cache frequently accessed data
- Optimize database queries

## 🚀 CI/CD Integration

### GitHub Actions Workflow

```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3

    - name: Set up Java
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Build application
      run: ./mvnw clean package -DskipTests

    - name: Build Docker image
      run: docker build -t stock-service:${{ github.sha }} .

    - name: Deploy to Kubernetes
      run: |
        kubectl set image deployment/stocks-service \
          stocks-service=stock-service:${{ github.sha }}
        kubectl rollout status deployment/stocks-service
```

### Deployment Verification

```bash
# Verify deployment
make health

# Check all services
make status

# Run smoke tests
make test-smoke

# Check metrics
curl http://localhost:8080/actuator/prometheus
```

## 📚 References

### Documentation Links
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Spring Boot Docker Documentation](https://spring.io/guides/topicals/spring-boot-docker/)
- [Prometheus Operator](https://prometheus-operator.dev/)

### Configuration Files
- `docker-compose.yml` - Single compose file with profiles
- `Dockerfile` - Application container definition
- `k8s/` - Kubernetes manifests
- `monitoring/` - Monitoring configurations

---

**The deployment process is now simplified with a single Docker Compose file and profiles, making it easy to deploy in any environment! 🚀**
