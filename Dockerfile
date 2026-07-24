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
RUN apk add --no-cache wget
RUN mkdir -p /pos-data /connectors && chown pos:pos /pos-data /connectors
COPY --from=build /app/target/*.jar app.jar
COPY connectors/ /connectors/
EXPOSE 9090
HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD wget -qO- http://localhost:9090/actuator/health || exit 1
USER pos
ENTRYPOINT ["java", "-jar", "app.jar"]
