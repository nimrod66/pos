@echo off
setlocal

set JAR=target\POS-0.0.1-SNAPSHOT.jar
set JAVA_OPTS=-Xmx512m -Dspring.profiles.active=offline

if not exist "pos-data" mkdir pos-data
if not exist "%JAR%" (
    echo JAR not found — building...
    call mvnw.cmd package -DskipTests -q
    if %errorlevel% neq 0 exit /b %errorlevel%
)

if not defined SERVER_PORT set SERVER_PORT=9090

echo Starting Pharmacy POS (offline / H2 terminal mode)...
echo Database: pos-data\terminal.mv.db
echo H2 Console: http://localhost:%SERVER_PORT%/h2-console
echo Actuator:  http://localhost:%SERVER_PORT%/actuator/health

java %JAVA_OPTS% -jar "%JAR%" --server.port=%SERVER_PORT%
