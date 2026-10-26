# ==========================================
# Multi-stage Dockerfile for ZAI 26Z Air Quality API
# ==========================================

# ------------------------------------------
# Stage 1: Build the application JAR
# ------------------------------------------
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and POM first for layer caching
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Make wrapper executable
RUN chmod +x ./mvnw

# Copy source code
COPY src src

# Build package without running tests during image build
RUN ./mvnw clean package -DskipTests

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
