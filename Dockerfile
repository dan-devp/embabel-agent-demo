# syntax=docker/dockerfile:1
# Stage 1: Build
FROM gradle:8.14-jdk21 AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew gradlew.bat settings.gradle build.gradle ./
RUN chmod +x gradlew
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies --no-daemon
COPY src/ src/
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar --no-daemon -x test

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
