# Deployment Guide

This guide covers production deployment strategies, configuration management,
and operational practices for the Spring Boot + Pekko Actors Stock Service.

## 🚀 Deployment Overview

The application supports multiple deployment strategies:

- **Container Deployment** (Docker/Podman)
- **Kubernetes Deployment** (K8s/OpenShift)
- **Cloud Platform Deployment** (AWS, GCP, Azure)
- **Traditional VM Deployment** (JAR-based)

## 📦 Container Deployment

### Docker Configuration

#### Dockerfile (Multi-stage build)

```dockerfile

# Build stage

FROM maven:3.9.4-openjdk-21 as build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage

FROM openjdk:21-jre-slim
WORKDIR /app

# Create non-root user

RUN groupadd -r stocks && useradd -r -g stocks stocks

# Install security updates

RUN apt-get update && apt-get upgrade -y && rm -rf /var/lib/apt/lists/*

# Copy application

COPY --from=build /app/target/*.jar app.jar

# Set ownership

RUN chown stocks:stocks app.jar

# Switch to non-root user

USER stocks

# Health check

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f <http://localhost:8080/actuator/health> || exit 1

# Expose port

EXPOSE 8080

# Run application

ENTRYPOINT ["java", "-jar", "app.jar"]

```text

#### Docker Compose

```yaml

# docker-compose.yml

version: '3.8'

services:
  stocks-service:
    build: .
    ports:

      - "8080:8080"
    environment:

      - SPRING_PROFILES_ACTIVE=docker
      - FINNHUB_KEY=${FINNHUB_KEY}
      - ALPHA_VANTAGE_KEY=${ALPHA_VANTAGE_KEY}
      - JAVA_OPTS=-Xmx512m -Xms256m
    healthcheck:
      test: ["CMD", "curl", "-f", "<http://localhost:8080/actuator/health">]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    restart: unless-stopped

  # Optional: Add monitoring stack
  prometheus:
    image: prom/prometheus:latest
    ports:

      - "9090:9090"
    volumes:

      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
    depends_on:

      - stocks-service

  grafana:
    image: grafana/grafana:latest
    ports:

      - "3000:3000"
    environment:

      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:

      - grafana-storage:/var/lib/grafana
    depends_on:

      - prometheus

volumes:
  grafana-storage:

```text

#### Build and Run

```bash

# Build image

docker build -t stocks-service:latest .

# Run container

docker run -d \
  --name stocks-service \
  -p 8080:8080 \
  -e FINNHUB_KEY=${FINNHUB_KEY} \
  -e SPRING_PROFILES_ACTIVE=docker \
  stocks-service:latest

# Run with Docker Compose

docker-compose up -d

# View logs

docker-compose logs -f stocks-service

# Scale service

docker-compose up -d --scale stocks-service=3

```text

## ☸️ Kubernetes Deployment

### Kubernetes Manifests

#### Namespace

```yaml

# k8s/namespace.yaml

apiVersion: v1
kind: Namespace
metadata:
  name: stocks-service
  labels:
    app: stocks-service

```text

#### ConfigMap

```yaml

# k8s/configmap.yaml

apiVersion: v1
kind: ConfigMap
metadata:
  name: stocks-config
  namespace: stocks-service
data:
  application.yml: |
    spring:
      profiles:
        active: kubernetes

    management:
      endpoints:
        web:
          exposure:
            include: health,prometheus,info
      endpoint:
        health:
          show-details: always

    logging:
      level:
        com.techishthoughts.stocks: INFO
        org.apache.pekko: WARN

```text

#### Secret

```yaml

# k8s/secret.yaml

apiVersion: v1
kind: Secret
metadata:
  name: stocks-secrets
  namespace: stocks-service
type: Opaque
data:
  finnhub-key: <base64-encoded-key>
  alpha-vantage-key: <base64-encoded-key>

```text

#### Deployment

```yaml

# k8s/deployment.yaml

apiVersion: apps/v1
kind: Deployment
metadata:
  name: stocks-service
  namespace: stocks-service
  labels:
    app: stocks-service
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
          protocol: TCP
        env:

        - name: SPRING_PROFILES_ACTIVE
          value: "kubernetes"

        - name: FINNHUB_KEY
          valueFrom:
            secretKeyRef:
              name: stocks-secrets
              key: finnhub-key

        - name: ALPHA_VANTAGE_KEY
          valueFrom:
            secretKeyRef:
              name: stocks-secrets
              key: alpha-vantage-key

        - name: JAVA_OPTS
          value: "-Xmx1g -Xms512m -XX:+UseG1GC"
        volumeMounts:

        - name: config-volume
          mountPath: /app/config
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
      volumes:

      - name: config-volume
        configMap:
          name: stocks-config
      restartPolicy: Always

```text

#### Service

```yaml

# k8s/service.yaml

apiVersion: v1
kind: Service
metadata:
  name: stocks-service
  namespace: stocks-service
  labels:
    app: stocks-service
spec:
  selector:
    app: stocks-service
  ports:

  - name: http
    port: 80
    targetPort: 8080
    protocol: TCP
  type: ClusterIP

```text

#### Ingress

```yaml

# k8s/ingress.yaml

apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: stocks-ingress
  namespace: stocks-service
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:

  - hosts:
    - stocks-api.example.com
    secretName: stocks-tls
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
              number: 80

```text

#### HorizontalPodAutoscaler

```yaml

# k8s/hpa.yaml

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

```text

### Deploy to Kubernetes

```bash

# Create namespace and apply manifests

kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml
kubectl apply -f k8s/hpa.yaml

# Verify deployment

kubectl get pods -n stocks-service
kubectl get services -n stocks-service
kubectl get ingress -n stocks-service

# View logs

kubectl logs -f deployment/stocks-service -n stocks-service

# Scale deployment

kubectl scale deployment stocks-service --replicas=5 -n stocks-service

```text

## ☁️ Cloud Platform Deployment

### AWS Deployment

#### ECS with Fargate

```json
{
  "family": "stocks-service",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024",
  "memory": "2048",
  "executionRoleArn": "arn:aws:iam::account:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::account:role/ecsTaskRole",
  "containerDefinitions": [
    {
      "name": "stocks-service",
"image": "your-account.dkr.ecr.region.amazonaws.com/stocks-service:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "aws"
        }
      ],
      "secrets": [
        {
          "name": "FINNHUB_KEY",
"valueFrom": "arn:aws:secretsmanager:region:account:secret:stocks/finnhub-key"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/stocks-service",
          "awslogs-region": "us-west-2",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": [
          "CMD-SHELL",
          "curl -f <http://localhost:8080/actuator/health> || exit 1"
        ],
        "interval": 30,
        "timeout": 10,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}

```text

#### EKS Deployment

```bash

# Create EKS cluster

eksctl create cluster --name stocks-cluster --region us-west-2

# Apply Kubernetes manifests

kubectl apply -f k8s/

# Install AWS Load Balancer Controller

helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  --set clusterName=stocks-cluster \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller

```text

### Google Cloud Platform

#### Cloud Run Deployment

```yaml

# cloudrun.yaml

apiVersion: serving.knative.dev/v1
kind: Service
metadata:
  name: stocks-service
  annotations:
    run.googleapis.com/ingress: all
spec:
  template:
    metadata:
      annotations:
        autoscaling.knative.dev/maxScale: "10"
        autoscaling.knative.dev/minScale: "1"
        run.googleapis.com/cpu-throttling: "false"
        run.googleapis.com/execution-environment: gen2
    spec:
      containerConcurrency: 80
      timeoutSeconds: 300
      containers:

      - image: gcr.io/your-project/stocks-service:latest
        ports:

        - containerPort: 8080
        env:

        - name: SPRING_PROFILES_ACTIVE
          value: "gcp"

        - name: FINNHUB_KEY
          valueFrom:
            secretKeyRef:
              name: stocks-secrets
              key: finnhub-key
        resources:
          limits:
            cpu: "2"
            memory: "2Gi"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30

```text

```bash

# Deploy to Cloud Run

gcloud run services replace cloudrun.yaml --region=us-central1

```text

### Azure Container Instances

```yaml

# azure-container-instance.yaml

apiVersion: 2021-03-01
location: eastus
name: stocks-service
properties:
  containers:

  - name: stocks-service
    properties:
      image: youracr.azurecr.io/stocks-service:latest
      ports:

      - port: 8080
        protocol: TCP
      environmentVariables:

      - name: SPRING_PROFILES_ACTIVE
        value: azure

      - name: FINNHUB_KEY
        secureValue: your-finnhub-key
      resources:
        requests:
          cpu: 1
          memoryInGB: 2
  osType: Linux
  restartPolicy: Always
  ipAddress:
    type: Public
    ports:

    - protocol: TCP
      port: 8080
  sku: Standard

```text

## 🔧 Configuration Management

### Environment-Specific Configurations

#### application-docker.yml

```yaml
spring:
  profiles:
    active: docker

logging:
  level:
    com.techishthoughts.stocks: INFO
    org.apache.pekko: WARN

management:
  endpoints:
    web:
      exposure:
        include: health,prometheus,info,metrics

# Docker-specific settings

server:
  shutdown: graceful
  tomcat:
    threads:
      max: 200
      min-spare: 10

```text

#### application-kubernetes.yml

```yaml
spring:
  profiles:
    active: kubernetes

# Kubernetes-specific configuration

management:
  endpoint:
    health:
      probes:
        enabled: true
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true

# Actor system configuration for Kubernetes

pekko:
  actor:
    provider: cluster
  cluster:
    seed-nodes: ["pekko://stocks@stocks-service-0:2552"]
    min-nr-of-members: 3

```text

#### application-aws.yml

```yaml
spring:
  profiles:
    active: aws

# AWS-specific configuration

cloud:
  aws:
    region:
      auto: true
    credentials:
      instanceProfile: true

# CloudWatch logging

logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# AWS Parameter Store integration

aws:
  paramstore:
    enabled: true
    prefix: /stocks-service

```text

### Secret Management

#### Kubernetes Secrets

```bash

# Create secrets

kubectl create secret generic stocks-secrets \
  --from-literal=finnhub-key=your-key \
  --from-literal=alpha-vantage-key=your-key \
  -n stocks-service

# Use sealed secrets for GitOps

kubeseal --format=yaml < secret.yaml > sealed-secret.yaml

```text

#### AWS Secrets Manager

```bash

# Store secrets

aws secretsmanager create-secret \
  --name stocks/finnhub-key \
  --description "Finnhub API key for stocks service" \
  --secret-string "your-finnhub-api-key"

# IAM policy for ECS task

{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": [
        "arn:aws:secretsmanager:region:account:secret:stocks/*"
      ]
    }
  ]
}

```text

#### HashiCorp Vault Integration

```yaml

# application-vault.yml

spring:
  cloud:
    vault:
      uri: <https://vault.example.com:8200>
      authentication: KUBERNETES
      kubernetes:
        role: stocks-service
service-account-token-file: /var/run/secrets/kubernetes.io/serviceaccount/token
      kv:
        enabled: true
        backend: secret
        default-context: stocks-service

```text

## 📊 Monitoring and Alerting

### Prometheus Configuration

```yaml

# monitoring/prometheus.yml

global:
  scrape_interval: 15s

scrape_configs:

- job_name: 'stocks-service'
  static_configs:

  - targets: ['stocks-service:8080']
  metrics_path: /actuator/prometheus
  scrape_interval: 10s

rule_files:

  - "alert-rules.yml"

alerting:
  alertmanagers:

  - static_configs:
    - targets:
      - alertmanager:9093

```text

### Alert Rules

```yaml

# monitoring/alert-rules.yml

groups:

- name: stocks-service
  rules:

  - alert: HighErrorRate
    expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High error rate detected"
      description: "Error rate is {{ $value }} errors per second"

  - alert: HighMemoryUsage
    expr: process_memory_usage_bytes / process_memory_max_bytes > 0.9
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "High memory usage"
      description: "Memory usage is {{ $value | humanizePercentage }}"

  - alert: ActorSystemDown
    expr: up{job="stocks-service"} == 0
    for: 2m
    labels:
      severity: critical
    annotations:
      summary: "Stocks service is down"
      description: "Stocks service has been down for more than 2 minutes"

```text

### Grafana Dashboards

```json
{
  "dashboard": {
    "title": "Stocks Service Dashboard",
    "panels": [
      {
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count[5m])",
            "legendFormat": "{{ method }} {{ uri }}"
          }
        ]
      },
      {
        "title": "Response Time",
        "type": "graph",
        "targets": [
          {
"expr": "histogram_quantile(0.95,
rate(http_server_requests_seconds_bucket[5m]))",
            "legendFormat": "95th percentile"
          }
        ]
      },
      {
        "title": "Actor Message Processing",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(akka_actor_mailbox_size[5m])",
            "legendFormat": "{{ actor }}"
          }
        ]
      }
    ]
  }
}

```text

## 🚀 CI/CD Pipeline

### GitHub Actions

```yaml

# .github/workflows/deploy.yml

name: Deploy to Production

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:

    - uses: actions/checkout@v3
    - uses: actions/setup-java@v3
      with:
        java-version: '21'

    - name: Run tests
      run: ./mvnw test -Pci

  build:
    needs: test
    runs-on: ubuntu-latest
    steps:

    - uses: actions/checkout@v3
    - name: Build Docker image
      run: |
        docker build -t stocks-service:${{ github.sha }} .
        docker tag stocks-service:${{ github.sha }} stocks-service:latest

    - name: Push to registry
      run: |
echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u ${{
secrets.DOCKER_USERNAME }} --password-stdin
        docker push stocks-service:${{ github.sha }}
        docker push stocks-service:latest

  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:

    - name: Deploy to Kubernetes
      run: |
        kubectl set image deployment/stocks-service \
          stocks-service=stocks-service:${{ github.sha }} \
          -n stocks-service

```text

### GitLab CI/CD

```yaml

# .gitlab-ci.yml

stages:

  - test
  - build
  - deploy

variables:
  DOCKER_IMAGE: $CI_REGISTRY_IMAGE/stocks-service

test:
  stage: test
  image: maven:3.9.4-openjdk-21
  script:

    - ./mvnw test -Pci
  coverage: '/Total.*?([0-9]{1,3})%/'

build:
  stage: build
  image: docker:latest
  services:

    - docker:dind
  script:

    - docker build -t $DOCKER_IMAGE:$CI_COMMIT_SHA .
    - docker push $DOCKER_IMAGE:$CI_COMMIT_SHA
  only:

    - main

deploy:
  stage: deploy
  image: bitnami/kubectl:latest
  script:

    - kubectl set image deployment/stocks-service
        stocks-service=$DOCKER_IMAGE:$CI_COMMIT_SHA
        -n stocks-service
  only:

    - main

```text

## 🔒 Security Considerations

### Container Security

```dockerfile

# Security-hardened Dockerfile

FROM openjdk:21-jre-slim

# Install security updates

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Create non-root user

RUN groupadd -r stocks && useradd -r -g stocks stocks

# Set up application directory

WORKDIR /app
COPY target/*.jar app.jar
RUN chown stocks:stocks app.jar

# Switch to non-root user

USER stocks

# Security labels

LABEL security.contact="security@example.com"
LABEL security.policy="<https://example.com/security-policy">

# Use read-only root filesystem


# docker run --read-only stocks-service

```text

### Kubernetes Security

```yaml

# Security-enhanced deployment

apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
        seccompProfile:
          type: RuntimeDefault
      containers:

      - name: stocks-service
        securityContext:
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: true
          capabilities:
            drop:

            - ALL
        volumeMounts:

        - name: tmp-volume
          mountPath: /tmp

        - name: logs-volume
          mountPath: /app/logs
      volumes:

      - name: tmp-volume
        emptyDir: {}

      - name: logs-volume
        emptyDir: {}

```text

### Network Policies

```yaml

# k8s/network-policy.yaml

apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: stocks-service-network-policy
  namespace: stocks-service
spec:
  podSelector:
    matchLabels:
      app: stocks-service
  policyTypes:

  - Ingress
  - Egress
  ingress:

  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-system
    ports:

    - protocol: TCP
      port: 8080
  egress:

  - to: []
    ports:

    - protocol: TCP
      port: 443  # HTTPS to external APIs

    - protocol: TCP
      port: 53   # DNS

    - protocol: UDP
      port: 53   # DNS

```text

## 📈 Performance Tuning

### JVM Tuning

```bash

# Production JVM settings

JAVA_OPTS="-Xmx2g -Xms1g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UnlockExperimentalVMOptions \
  -XX:+UseCGroupMemoryLimitForHeap \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -Djava.security.egd=file:/dev/./urandom"

```text

### Actor System Tuning

```yaml

# application-production.yml

pekko:
  actor:
    default-dispatcher:
      type: "Dispatcher"
      executor: "fork-join-executor"
      fork-join-executor:
        parallelism-min: 8
        parallelism-factor: 2.0
        parallelism-max: 32
      throughput: 100

  http:
    server:
      max-connections: 1024
      request-timeout: 60s
      idle-timeout: 60s

```text
This deployment guide provides enterprise-grade deployment strategies with
comprehensive monitoring, security, and performance considerations for
production environments.
