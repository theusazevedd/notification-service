FROM gradle:8.14.4-jdk17-alpine AS build

WORKDIR /workspace

COPY notification-service /workspace/notification-service

# Build only the executable jar inside the container.
RUN gradle -p /workspace/notification-service clean bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

COPY --from=build /workspace/notification-service/build/libs/*.jar /app/notification-service.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/notification-service.jar"]