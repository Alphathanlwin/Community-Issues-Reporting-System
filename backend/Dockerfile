# Multi-stage build — Render has no native Java runtime, so Spring Boot apps
# there run as a container. Stage 1 compiles the jar with Maven; stage 2 is
# just a slim JRE running that jar, so the final image doesn't carry the
# entire Maven/JDK toolchain.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Dependencies first so `docker build` can cache this layer across source changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Non-root — good practice, and required by some hosts' container policies.
RUN addgroup -S scirs && adduser -S scirs -G scirs
COPY --from=build /app/target/scirs.jar app.jar
RUN mkdir -p /app/uploads && chown -R scirs:scirs /app
USER scirs

# Render injects PORT at runtime; application.properties already reads it
# (server.port=${PORT:${SERVER_PORT:8080}}). EXPOSE is documentation only.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
