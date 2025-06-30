# ============================================================================
# Multi-stage Dockerfile for Spring Boot + Pekko Actors Application
# ============================================================================

# ============================================================================
# Stage 1: Build Stage
# ============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set build metadata
LABEL stage=builder
LABEL description="Build stage for Spring Boot + Pekko Actors application"

# Install build dependencies
RUN apk add --no-cache \
    curl \
    git \
    && rm -rf /var/cache/apk/*

# Create application directory
WORKDIR /build

# Copy Maven wrapper and configuration first (for better layer caching)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (separate layer for better caching)
# Skip offline mode if there are network issues
RUN ./mvnw dependency:go-offline -B || echo "Offline dependency download failed, will download during build"

# Copy source code
COPY src/ src/

# Build the application
RUN ./mvnw clean package -DskipTests -B -o || ./mvnw clean package -DskipTests -B && \
    # Extract application jar for optimization
    mkdir -p extracted && \
    java -Djarmode=layertools -jar target/*.jar extract --destination extracted/

# ============================================================================
# Stage 2: Runtime Stage
# ============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Set metadata
LABEL maintainer="TechIsThoughts <tech@techishthoughts.com>"
LABEL description="Spring Boot + Pekko Actors Stock Service"
LABEL version="1.0.0"

# Install runtime dependencies and security updates
RUN apk add --no-cache \
    curl \
    dumb-init \
    tini \
    && apk upgrade \
    && rm -rf /var/cache/apk/* \
    && addgroup -g 1001 -S appgroup \
    && adduser -u 1001 -S appuser -G appgroup

# Create application directories
WORKDIR /app

# Create directories for logs and temp files
RUN mkdir -p /app/logs /app/tmp \
    && chown -R appuser:appgroup /app

# Copy application layers from builder stage (optimized for Docker layer caching)
COPY --from=builder --chown=appuser:appgroup /build/extracted/dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appgroup /build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/extracted/application/ ./

# Switch to non-root user for security
USER appuser:appgroup

# Configure JVM options for maximum performance with ZGC
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=80.0 \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseZGC \
    -XX:+UnlockDiagnosticVMOptions \
    -XX:+UseTransparentHugePages \
    -XX:+UseLargePages \
    -XX:+AlwaysPreTouch \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -XX:+UseCompressedOops \
    -XX:+UseCompressedClassPointers \
    -XX:+EnableDynamicAgentLoading \

    -XX:+UseNUMA \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=4 \
    -XX:+UseCodeCacheFlushing \
    -XX:ReservedCodeCacheSize=256m \
    -XX:InitialCodeCacheSize=64m \
    -XX:CompileThreshold=1000 \
    -XX:+UseCountedLoopSafepoints \
    -XX:+UseLoopPredicate \
    -XX:+RangeCheckElimination \
    -XX:+EliminateLocks \
    -XX:+DoEscapeAnalysis \
    -XX:+EliminateAllocations \
    -XX:+UseSuperWord \
    -XX:+UseVectorCmov \
    -XX:+UseAESIntrinsics \
    -XX:+UseSHA1Intrinsics \
    -XX:+UseSHA256Intrinsics \
    -XX:+UseSHA512Intrinsics \
    -XX:+UseAdler32Intrinsics \
    -XX:+UseCRC32Intrinsics \
    -XX:+UseCRC32CIntrinsics \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=docker \
    -Dlogging.config=classpath:logback-spring.xml"

# Configure application options
ENV APP_OPTS="--server.port=8080 \
    --management.server.port=8081 \
    --management.endpoints.web.exposure.include=health,info,metrics,prometheus \
    --logging.level.root=INFO \
    --logging.file.path=/app/logs"

# Configure Pekko-specific options
ENV PEKKO_OPTS="-Dpekko.actor.provider=local \
    -Dpekko.actor.default-dispatcher.throughput=10 \
    -Dpekko.log-config-on-start=off"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# Expose ports
EXPOSE 8080 8081

# Use tini as init system for proper signal handling
ENTRYPOINT ["tini", "--"]

# Start the application with optimized JVM settings
CMD ["sh", "-c", "exec java $JAVA_OPTS $PEKKO_OPTS org.springframework.boot.loader.launch.JarLauncher $APP_OPTS"]
