@echo off
setlocal
if "%~1"=="" (set PORT=8080) else (set PORT=%~1)
docker compose up --build -d
echo Stock Market Simulator running at http://localhost:%PORT%
endlocal
