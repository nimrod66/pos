#!/bin/bash
set -e

JAR="target/POS-0.0.1-SNAPSHOT.jar"
JAVA_OPTS="-Xmx512m"

if [ ! -f "$JAR" ]; then
    echo "JAR not found - building..."
    ./mvnw package -DskipTests -q
fi

echo "Starting Pharmacy POS local development node..."
echo "PostgreSQL must be available through SPRING_DATASOURCE_URL or localhost:5432."
echo "Actuator: http://localhost:9090/actuator/health"
echo "Swagger:  http://localhost:9090/swagger-ui/index.html"

java $JAVA_OPTS -jar "$JAR"
