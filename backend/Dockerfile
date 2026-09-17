# ─────────────────────────────────────────────────────────────────────────────
#  WildGuard Tracker  |  Multi-Stage Dockerfile
#  Stage 1: Build the Spring Boot fat-JAR with Gradle
#  Stage 2: Lean runtime image served on port 8080
# ─────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Build ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /workspace

# Copy Gradle wrapper and dependency descriptors first (layer-cache friendly)
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .

# Give the wrapper execute permissions (needed on Linux build agents)
RUN chmod +x gradlew

# Pre-download dependencies (cached unless build.gradle changes)
RUN ./gradlew dependencies --no-daemon -q || true

# Copy source and build the fat JAR, skipping tests for speed
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# ── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

LABEL maintainer="KWS WildGuard Team"
LABEL app="wildguard-tracker-backend"

# Non-root user for security
RUN addgroup -S kws && adduser -S kws -G kws

WORKDIR /app

# Pull only the fat JAR from the build stage
COPY --from=builder /workspace/build/libs/backend-0.0.1-SNAPSHOT.jar app.jar

# Change ownership so the non-root user can read the jar
RUN chown kws:kws app.jar

USER kws

# Render injects PORT env var; Spring Boot reads server.port from it.
# We explicitly set server.port here as a safe default.
ENV SERVER_PORT=8080
EXPOSE 8080

# JVM tuning flags for a low-memory Render Free/Starter instance
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
