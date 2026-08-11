@echo off
setlocal

set JAR=target\POS-0.0.1-SNAPSHOT.jar
set JAVA_OPTS=-Xmx512m

if not exist "%JAR%" (
    echo JAR not found - building...
    call mvnw.cmd package -DskipTests -q
    if %errorlevel% neq 0 exit /b %errorlevel%
)

echo Starting Pharmacy POS local development node...
echo PostgreSQL must be available through SPRING_DATASOURCE_URL or localhost:5432.
echo Actuator: http://localhost:9090/actuator/health
echo Swagger:  http://localhost:9090/swagger-ui/index.html

java %JAVA_OPTS% -jar "%JAR%"
