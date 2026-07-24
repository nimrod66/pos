#!/bin/bash
set -e

JAR="target/POS-0.0.1-SNAPSHOT.jar"
JAVA_OPTS="-Xmx512m -Dspring.profiles.active=offline"
PORT="${SERVER_PORT:-9090}"

mkdir -p pos-data
if [ ! -f "$JAR" ]; then
    echo "JAR not found — building..."
    ./mvnw package -DskipTests -q
fi

echo "Starting Pharmacy POS (offline / H2 terminal mode)..."
echo "Database: pos-data/terminal.mv.db"
echo "H2 Console: http://localhost:$PORT/h2-console"
echo "Actuator:  http://localhost:$PORT/actuator/health"

java $JAVA_OPTS -jar "$JAR" --server.port="$PORT"
