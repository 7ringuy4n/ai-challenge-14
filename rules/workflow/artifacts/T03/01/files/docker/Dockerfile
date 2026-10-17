# Build the executable jar with the pinned Java 21/Maven toolchain. Setup builds
# the image but does not execute the test suite; tests remain an explicit gate.
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src/ src/
COPY sql/ sql/
COPY config/ config/
RUN mvn -B -DskipTests package

# Keep the runtime image small and run the application without root privileges.
FROM eclipse-temurin:21-jre-jammy AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 claims \
    && useradd --system --uid 10001 --gid claims --home-dir /app --shell /usr/sbin/nologin claims
WORKDIR /app
COPY --from=build --chown=claims:claims /workspace/target/claims-workflow.jar /app/claims-workflow.jar
COPY --from=build --chown=claims:claims /workspace/config/workflow.yml /app/config/workflow.yml
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=6 \
  CMD curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/claims-workflow.jar"]
