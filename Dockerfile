# build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN ./gradlew --no-daemon dependencies > /dev/null
COPY src ./src
RUN ./gradlew --no-daemon installDist

# run
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/build/install/task_tracker/lib ./lib
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "/app/lib/*"]
CMD ["ru.hse.tracker.AppKt"]
