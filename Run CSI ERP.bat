@echo off
REM Starts the CSI ERP system as one app: brings up the backend server
REM minimized in the background, waits until it's actually ready, then
REM launches the desktop (JavaFX) window. Double-click this file - or,
REM better, the "CSI ERP" shortcut on the Desktop - instead of running
REM two separate mvnw commands by hand.
REM
REM Requires MySQL80 to already be running (Win+R -> services.msc -> start
REM it, or "net start MYSQL80" from an admin terminal) - this script only
REM starts the app, not the database.

cd /d "%~dp0"

echo Starting backend server...
start /min "CSI ERP Backend" cmd /k "cd backend && mvnw.cmd spring-boot:run"

echo Waiting for backend to be ready (this can take up to a minute)...
set /a tries=0
:waitloop
set /a tries+=1
if %tries% gtr 40 (
    echo.
    echo Backend still isn't responding after 80 seconds.
    echo Look for the minimized "CSI ERP Backend" window in your taskbar
    echo and open it to check for errors - a common cause is MySQL not
    echo running yet.
    pause
    exit /b 1
)
timeout /t 2 /nobreak >nul
curl -s -o nul http://localhost:8080/api/dashboard/summary
if errorlevel 1 goto waitloop

echo Backend is up. Launching the app...
cd frontend
call mvnw.cmd javafx:run
