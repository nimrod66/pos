#!/bin/bash
set -e

JAR="target/POS-0.0.1-SNAPSHOT.jar"
JAVA_OPTS="-Xmx512m"

if [ ! -f "$JAR" ]; then
    echo "JAR not found — building..."
    ./mvnw package -DskipTests -q
fi

echo "Starting Pharmacy POS (online / dev mode)..."
echo "Actuator: http://localhost:9090/actuator/health"
echo "API:      http://localhost:9090"

java $JAVA_OPTS -jar "$JAR"
