#!/bin/bash
set -e

JAR="target/POS-0.0.1-SNAPSHOT.jar"
JAVA_OPTS="-Xmx512m -Dspring.profiles.active=offline"
PORT="${SERVER_PORT:-9090}"

if [ ! -f "$JAR" ]; then
    echo "JAR not found - building..."
    ./mvnw package -DskipTests -q
fi

echo "Starting Pharmacy POS local-only node..."
echo "PostgreSQL must be available through SPRING_DATASOURCE_URL or localhost:5432."
echo "Actuator: http://localhost:$PORT/actuator/health"
echo "Swagger:  http://localhost:$PORT/swagger-ui/index.html"

java $JAVA_OPTS -jar "$JAR" --server.port="$PORT"
