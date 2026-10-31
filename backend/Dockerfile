FROM eclipse-temurin:25-jdk AS builder

WORKDIR /build

COPY pom.xml mvnw ./
COPY .mvn ./.mvn
RUN ./mvnw dependency:go-offline -B || true

COPY src ./src

RUN ./mvnw test

RUN ./mvnw package -DskipTests

FROM eclipse-temurin:25-jre AS runner

WORKDIR /app

RUN if command -v apt-get >/dev/null 2>&1; then \
        apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*; \
    fi && \
    if command -v groupadd >/dev/null 2>&1; then \
        groupadd -r appgroup && useradd -r -g appgroup -d /app -s /bin/false appuser; \
    else \
        addgroup -S appgroup && adduser -S -G appgroup -h /app -s /bin/false appuser; \
    fi

RUN mkdir -p /app/data && chown -R appuser:appgroup /app

COPY --from=builder --chown=appuser:appgroup /build/target/air-quality-api-1.0.0.jar app.jar

USER appuser

EXPOSE 8080

ENV SERVER_PORT=8080 \
    ADMIN_USERNAME=admin \
    ADMIN_PASSWORD=Admin123! \
    ZAI_SEED=true

HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -f -s http://127.0.0.1:${SERVER_PORT}/api/health || wget --quiet --tries=1 --spider http://127.0.0.1:${SERVER_PORT}/api/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
