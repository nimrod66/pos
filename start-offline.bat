@echo off
setlocal

set JAR=target\POS-0.0.1-SNAPSHOT.jar
set JAVA_OPTS=-Xmx512m -Dspring.profiles.active=offline

if not exist "%JAR%" (
    echo JAR not found - building...
    call mvnw.cmd package -DskipTests -q
    if %errorlevel% neq 0 exit /b %errorlevel%
)

if not defined SERVER_PORT set SERVER_PORT=9090

echo Starting Pharmacy POS local-only node...
echo PostgreSQL must be available through SPRING_DATASOURCE_URL or localhost:5432.
echo Actuator: http://localhost:%SERVER_PORT%/actuator/health
echo Swagger:  http://localhost:%SERVER_PORT%/swagger-ui/index.html

java %JAVA_OPTS% -jar "%JAR%" --server.port=%SERVER_PORT%
