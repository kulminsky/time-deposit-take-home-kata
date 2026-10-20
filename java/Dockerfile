# Builds and runs the application without a local JDK; `docker compose up` (compose.yaml) uses it.

# Build stage: the Maven wrapper (Maven 3.9.16) on the Java 17 baseline.
FROM eclipse-temurin:17.0.20.1_1-jdk-noble AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
# The cache mount keeps the Maven repository (and the wrapper's Maven download) between builds.
# Tests are not compiled or run here: the integration tests need Docker (Testcontainers). `./mvnw verify` and CI
# run them.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp package -Dmaven.test.skip=true

# Runtime stage: only a JRE and the executable jar.
FROM eclipse-temurin:17.0.20.1_1-jre-noble
RUN useradd --system app
USER app
WORKDIR /app
# Matches only the executable jar, not the *.jar.original that the Spring Boot plugin leaves next to it.
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
