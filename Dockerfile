FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S pos && adduser -S pos -G pos
RUN mkdir -p /pos-data && chown pos:pos /pos-data
COPY --from=build /app/target/*.jar app.jar
USER pos
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
