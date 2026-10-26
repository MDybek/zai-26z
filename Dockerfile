# ==========================================
# Multi-stage Dockerfile for ZAI 26Z Air Quality API
# Compatible with Ubuntu/Debian (Jammy) and Alpine
# ==========================================

# ------------------------------------------
# Stage 1: Build the application JAR
# ------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21-jammy AS builder

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
FROM eclipse-temurin:21-jre-jammy AS runner

WORKDIR /app

# Install curl on Debian/Ubuntu for healthchecks and setup non-root user
RUN if command -v apt-get >/dev/null 2>&1; then \
        apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*; \
    fi && \
    if command -v groupadd >/dev/null 2>&1; then \
        groupadd -r appgroup && useradd -r -g appgroup -d /app -s /bin/false appuser; \
    else \
        addgroup -S appgroup && adduser -S -G appgroup -h /app -s /bin/false appuser; \
    fi

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

# Container healthcheck (supports curl on Debian/Ubuntu and wget on Alpine)
HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -f -s http://127.0.0.1:${SERVER_PORT}/api/health || wget --quiet --tries=1 --spider http://127.0.0.1:${SERVER_PORT}/api/health || exit 1

# Execute Spring Boot application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
