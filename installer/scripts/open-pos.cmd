@echo off
set "INSTALL_DIR=%~dp0..\.."
powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "%~dp0start-pilot.ps1" -InstallDir "%INSTALL_DIR%"
exit /b %ERRORLEVEL%
