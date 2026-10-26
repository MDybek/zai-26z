# ==========================================
# Multi-stage Dockerfile for ZAI 26Z Air Quality API
# ==========================================

# ------------------------------------------
# Stage 1: Build the application JAR
# ------------------------------------------
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copy POM and go-offline for layer caching
COPY pom.xml ./
RUN mvn dependency:go-offline -B || true

# Copy source code and compile JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ------------------------------------------
# Stage 2: Minimal, secure runtime container
# ------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runner

WORKDIR /app

# Create a non-root user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create persistent data directory with proper ownership
RUN mkdir -p /app/data && chown -R appuser:appgroup /app

# Copy the built artifact from builder stage
COPY --from=builder --chown=appuser:appgroup /build/target/air-quality-api-1.0.0.jar app.jar

# Switch to non-root user
USER appuser

# Expose standard application port
EXPOSE 8080

# Environment defaults
ENV SERVER_PORT=8080 \
    ZAI_DB_PATH=/app/data/airquality.db \
    ADMIN_USERNAME=admin \
    ADMIN_PASSWORD=Admin123! \
    ZAI_SEED=true

# Container healthcheck
HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://127.0.0.1:${SERVER_PORT}/api/health || exit 1

# Execute Spring Boot application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
