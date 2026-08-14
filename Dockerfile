FROM eclipse-temurin:21-jdk AS build

WORKDIR /app
COPY backend/gradlew backend/build.gradle backend/settings.gradle ./
COPY backend/gradle ./gradle
RUN chmod +x gradlew
COPY backend/src ./src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["sh", "-c", "exec java -jar /app/app.jar"]
