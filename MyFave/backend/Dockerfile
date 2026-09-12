# Stage 1: Build
FROM gradle:8.11-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle lombok.config ./
COPY src ./src
RUN gradle bootJar --no-daemon

# Stage 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
