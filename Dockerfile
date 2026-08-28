FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
# Annotation processor paths are not included by dependency:go-offline.
RUN ./mvnw dependency:get -Dartifact=org.mapstruct:mapstruct-processor:1.6.3 -B
COPY src src
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S pos && adduser -S pos -G pos
RUN apk add --no-cache wget postgresql17-client su-exec
RUN mkdir -p /app/pos-data /app/backups /connectors && chown -R pos:pos /app/pos-data /app/backups /connectors
COPY --from=build /app/target/*.jar app.jar
COPY --chown=pos:pos connectors/ /connectors/
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
EXPOSE 9090
HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD wget -qO- http://localhost:9090/actuator/health || exit 1
ENTRYPOINT ["/entrypoint.sh"]
