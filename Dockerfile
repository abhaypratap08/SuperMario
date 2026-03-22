FROM eclipse-temurin:11-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew fatJar

FROM eclipse-temurin:11-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/SuperMario.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
