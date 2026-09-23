@echo off
setlocal EnableExtensions

title Widgify Launcher

echo.
echo ==========================================
echo              WIDGIFY LAUNCHER
echo ==========================================
echo.

REM ==========================================
REM Configuration
REM ==========================================

set "PROJECT_DIR=%~dp0"
set "MYSQL_HOME=C:\dev\mysql-8.0.37-winx64"
set "MYSQL_DATA=%MYSQL_HOME%\data"
set "TOMCAT_HOME=C:\dev\apache-tomcat-10.1.20"

cd /d "%PROJECT_DIR%"

echo [1/5] Checking MySQL...

netstat -ano | findstr ":3306" >nul

if %ERRORLEVEL% EQU 0 (
    echo MySQL is already running.
) else (
    echo Starting MySQL...

    start "Widgify MySQL" /min ^
        "%MYSQL_HOME%\bin\mysqld.exe" ^
        --datadir="%MYSQL_DATA%"

    echo Waiting for MySQL...

    :MYSQL_WAIT
    timeout /t 2 /nobreak >nul

    netstat -ano | findstr ":3306" >nul

    if %ERRORLEVEL% NEQ 0 (
        echo Still waiting for MySQL...
        goto MYSQL_WAIT
    )

    echo MySQL started successfully.
)

echo.
echo [2/5] Building Widgify...

call "%PROJECT_DIR%mvnw.cmd" package

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ==========================================
    echo BUILD FAILED
    echo ==========================================
    pause
    exit /b 1
)

echo Build successful.

echo.
echo [3/5] Preparing Tomcat...

set "CATALINA_HOME=%TOMCAT_HOME%"
set "CATALINA_BASE=%TOMCAT_HOME%"

REM Stop existing Tomcat if running
if exist "%TOMCAT_HOME%\bin\shutdown.bat" (
    call "%TOMCAT_HOME%\bin\shutdown.bat" >nul 2>&1
)

REM Give Tomcat time to shut down
timeout /t 3 /nobreak >nul

REM Remove old deployed application
if exist "%TOMCAT_HOME%\webapps\widgify" (
    rmdir /s /q "%TOMCAT_HOME%\webapps\widgify"
)

if exist "%TOMCAT_HOME%\webapps\widgify.war" (
    del /f /q "%TOMCAT_HOME%\webapps\widgify.war"
)

echo Deploying fresh WAR...

copy /y "%PROJECT_DIR%target\widgify.war" ^
    "%TOMCAT_HOME%\webapps\widgify.war" >nul

if %ERRORLEVEL% NEQ 0 (
    echo Failed to copy WAR to Tomcat.
    pause
    exit /b 1
)

echo WAR deployed.

echo.
echo [4/5] Starting Tomcat...

start "Widgify Tomcat" /min ^
    "%TOMCAT_HOME%\bin\catalina.bat" run

echo Waiting for Tomcat...

:SERVER_WAIT
timeout /t 2 /nobreak >nul

netstat -ano | findstr ":8080" >nul

if %ERRORLEVEL% NEQ 0 (
    echo Still waiting for Tomcat...
    goto SERVER_WAIT
)

echo Tomcat started successfully.

echo.
echo [5/5] Starting Widgify Desktop Client...

timeout /t 2 /nobreak >nul

start "Widgify Desktop" cmd /k ^
    "cd /d ""%PROJECT_DIR%"" && call mvnw.cmd exec:java ""-Dexec.mainClass=com.widgify.desktop.WidgifyDesktopClient"""

echo.
echo ==========================================
echo          WIDGIFY STARTED SUCCESSFULLY
echo ==========================================
echo.
echo MySQL  : localhost:3306
echo Tomcat : localhost:8080
echo App    : JavaFX Desktop Client
echo.
echo You can close this launcher window.
echo ==========================================
echo.

timeout /t 5 /nobreak >nul
exit /b 0