@echo off
setlocal EnableExtensions EnableDelayedExpansion

title Widgify Launcher

echo.
echo ============================================================
echo                         WIDGIFY
echo              Portable Application Launcher
echo ============================================================
echo.

REM ============================================================
REM PROJECT / LOCAL RUNTIME CONFIGURATION
REM ============================================================

set "PROJECT_DIR=%~dp0"
set "RUNTIME_DIR=%PROJECT_DIR%.widgify"

set "MYSQL_VERSION=8.0.37"
set "MYSQL_DIR=%RUNTIME_DIR%\mysql"
set "MYSQL_DATA=%MYSQL_DIR%\data"
set "MYSQL_BIN=%MYSQL_DIR%\bin"
set "MYSQL_EXE=%MYSQL_BIN%\mysqld.exe"
set "MYSQL_CLIENT=%MYSQL_BIN%\mysql.exe"

set "MYSQL_PORT=3306"
set "MYSQL_DATABASE=widgify"
set "MYSQL_USER=root"
set "MYSQL_PASSWORD="

set "TOMCAT_VERSION=10.1.20"
set "TOMCAT_DIR=%RUNTIME_DIR%\tomcat"

set "TOMCAT_PORT=8080"
set "TOMCAT_WEBAPPS=%TOMCAT_DIR%\webapps"

set "WAR_FILE=%PROJECT_DIR%target\widgify.war"
set "TOMCAT_WAR=%TOMCAT_WEBAPPS%\widgify.war"
set "TOMCAT_APP=%TOMCAT_WEBAPPS%\widgify"

cd /d "%PROJECT_DIR%"

REM ============================================================
REM CREATE LOCAL RUNTIME DIRECTORY
REM ============================================================

if not exist "%RUNTIME_DIR%" (
    mkdir "%RUNTIME_DIR%"
)

REM ============================================================
REM [1/8] CHECK JAVA
REM ============================================================

echo [1/8] Checking Java...

where java >nul 2>&1

if errorlevel 1 (
    echo.
    echo ERROR: Java was not found.
    echo.
    echo Widgify requires JDK 17 or newer.
    echo Please install a JDK and make sure JAVA_HOME is configured.
    echo.
    pause
    exit /b 1
)

java -version

echo Java found.
echo.

REM ============================================================
REM [2/8] DOWNLOAD MYSQL IF NECESSARY
REM ============================================================

echo [2/8] Checking portable MySQL...

if exist "%MYSQL_EXE%" (
    echo Portable MySQL already exists.
    goto MYSQL_INSTALLED
)

echo MySQL was not found.
echo.

echo Downloading MySQL %MYSQL_VERSION%...

set "MYSQL_ZIP=%RUNTIME_DIR%\mysql.zip"

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://dev.mysql.com/get/Downloads/MySQL-8.0/mysql-8.0.37-winx64.zip' -OutFile '%MYSQL_ZIP%'"

if errorlevel 1 (
    echo.
    echo ERROR: Failed to download MySQL.
    echo Check your internet connection.
    echo.
    pause
    exit /b 1
)

echo MySQL download completed.
echo Extracting MySQL...

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "Expand-Archive -Path '%MYSQL_ZIP%' -DestinationPath '%RUNTIME_DIR%\mysql_extract' -Force"

if errorlevel 1 (
    echo.
    echo ERROR: Failed to extract MySQL.
    echo.
    pause
    exit /b 1
)

if exist "%MYSQL_DIR%" (
    rmdir /s /q "%MYSQL_DIR%"
)

for /d %%D in ("%RUNTIME_DIR%\mysql_extract\mysql-*") do (
    move "%%D" "%MYSQL_DIR%" >nul
)

rmdir /s /q "%RUNTIME_DIR%\mysql_extract"
del /f /q "%MYSQL_ZIP%" >nul 2>&1

:MYSQL_INSTALLED

if not exist "%MYSQL_EXE%" (
    echo.
    echo ERROR: MySQL installation is incomplete.
    echo.
    pause
    exit /b 1
)

echo MySQL is available.
echo.

REM ============================================================
REM [3/8] INITIALIZE MYSQL DATA DIRECTORY
REM ============================================================

echo [3/8] Preparing MySQL database...

if not exist "%MYSQL_DATA%\mysql" (

    echo Initializing MySQL data directory...

    if exist "%MYSQL_DATA%" (
        rmdir /s /q "%MYSQL_DATA%"
    )

    mkdir "%MYSQL_DATA%"

    "%MYSQL_EXE%" ^
        --initialize-insecure ^
        --basedir="%MYSQL_DIR%" ^
        --datadir="%MYSQL_DATA%"

    if errorlevel 1 (
        echo.
        echo ERROR: MySQL initialization failed.
        echo.
        pause
        exit /b 1
    )

    echo MySQL initialized successfully.
) else (
    echo Existing local MySQL data directory found.
)

echo.

REM ============================================================
REM [4/8] START MYSQL
REM ============================================================

echo [4/8] Starting MySQL...

netstat -ano | findstr /R /C:":%MYSQL_PORT% .*LISTENING" >nul 2>&1

if not errorlevel 1 (
    echo MySQL is already running.
    goto MYSQL_READY
)

echo Starting portable MySQL...

start "Widgify - MySQL" /min ^
    "%MYSQL_EXE%" ^
    --console ^
    --basedir="%MYSQL_DIR%" ^
    --datadir="%MYSQL_DATA%" ^
    --port=%MYSQL_PORT% ^
    --bind-address=127.0.0.1

set /a MYSQL_TRIES=0

:MYSQL_WAIT

timeout /t 2 /nobreak >nul

netstat -ano | findstr /R /C:":%MYSQL_PORT% .*LISTENING" >nul 2>&1

if not errorlevel 1 (
    goto MYSQL_READY
)

set /a MYSQL_TRIES+=1

if !MYSQL_TRIES! GEQ 30 (
    echo.
    echo ERROR: MySQL failed to start within 60 seconds.
    echo Check the MySQL window for errors.
    echo.
    pause
    exit /b 1
)

echo Waiting for MySQL... !MYSQL_TRIES!/30
goto MYSQL_WAIT


:MYSQL_READY

echo MySQL is running.
echo.

REM ============================================================
REM INITIALIZE WIDGIFY DATABASE
REM ============================================================

echo Initializing Widgify database...

"%MYSQL_CLIENT%" ^
    --host=127.0.0.1 ^
    --port=%MYSQL_PORT% ^
    --user=%MYSQL_USER% ^
    --password=%MYSQL_PASSWORD% ^
    < "%PROJECT_DIR%database\schema.sql"

if errorlevel 1 (
    echo.
    echo ERROR: Database initialization failed.
    echo.
    pause
    exit /b 1
)

echo Database schema applied successfully.
echo.

REM ============================================================
REM [5/8] DOWNLOAD TOMCAT IF NECESSARY
REM ============================================================

echo [5/8] Checking portable Apache Tomcat...

if exist "%TOMCAT_DIR%\bin\catalina.bat" (
    echo Portable Tomcat already exists.
    goto TOMCAT_INSTALLED
)

echo Tomcat was not found.
echo.
echo Downloading Apache Tomcat %TOMCAT_VERSION%...

set "TOMCAT_ZIP=%RUNTIME_DIR%\tomcat.zip"

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://archive.apache.org/dist/tomcat/tomcat-10/v10.1.20/bin/apache-tomcat-10.1.20.zip' -OutFile '%TOMCAT_ZIP%'"

if errorlevel 1 (
    echo.
    echo ERROR: Failed to download Apache Tomcat.
    echo Check your internet connection.
    echo.
    pause
    exit /b 1
)

echo Tomcat download completed.
echo Extracting Tomcat...

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "Expand-Archive -Path '%TOMCAT_ZIP%' -DestinationPath '%RUNTIME_DIR%\tomcat_extract' -Force"

if errorlevel 1 (
    echo.
    echo ERROR: Failed to extract Tomcat.
    echo.
    pause
    exit /b 1
)

if exist "%TOMCAT_DIR%" (
    rmdir /s /q "%TOMCAT_DIR%"
)

for /d %%D in ("%RUNTIME_DIR%\tomcat_extract\apache-tomcat-*") do (
    move "%%D" "%TOMCAT_DIR%" >nul
)

rmdir /s /q "%RUNTIME_DIR%\tomcat_extract"
del /f /q "%TOMCAT_ZIP%" >nul 2>&1

:TOMCAT_INSTALLED

if not exist "%TOMCAT_DIR%\bin\catalina.bat" (
    echo.
    echo ERROR: Tomcat installation is incomplete.
    echo.
    pause
    exit /b 1
)

echo Tomcat is available.
echo.

REM ============================================================
REM [6/8] BUILD WIDGIFY
REM ============================================================

echo [6/8] Building Widgify...
echo.
echo Maven Wrapper will automatically download Maven and
echo all dependencies declared in pom.xml.
echo.

call "%PROJECT_DIR%mvnw.cmd" clean package

if errorlevel 1 (
    echo.
    echo ============================================================
    echo                       BUILD FAILED
    echo ============================================================
    echo.
    pause
    exit /b 1
)

if not exist "%WAR_FILE%" (
    echo.
    echo ERROR: widgify.war was not created.
    echo.
    pause
    exit /b 1
)

echo.
echo Build completed successfully.
echo.

REM ============================================================
REM [7/8] DEPLOY AND START TOMCAT
REM ============================================================

echo [7/8] Deploying Widgify server...

set "CATALINA_HOME=%TOMCAT_DIR%"
set "CATALINA_BASE=%TOMCAT_DIR%"

REM Stop existing portable Tomcat

netstat -ano | findstr /R /C:":%TOMCAT_PORT% .*LISTENING" >nul 2>&1

if not errorlevel 1 (
    echo Existing Tomcat detected.
    echo Stopping it...

    call "%TOMCAT_DIR%\bin\shutdown.bat" >nul 2>&1

    timeout /t 5 /nobreak >nul
)

REM Remove old application

if exist "%TOMCAT_APP%" (
    rmdir /s /q "%TOMCAT_APP%"
)

if exist "%TOMCAT_WAR%" (
    del /f /q "%TOMCAT_WAR%"
)

REM Deploy new WAR

copy /y "%WAR_FILE%" "%TOMCAT_WAR%" >nul

if errorlevel 1 (
    echo.
    echo ERROR: Failed to deploy WAR.
    echo.
    pause
    exit /b 1
)

echo WAR deployed successfully.
echo.

REM Start Tomcat

echo Starting Apache Tomcat...

start "Widgify - Tomcat" /min cmd /c ^
    ""%TOMCAT_DIR%\bin\catalina.bat" run"

set /a TOMCAT_TRIES=0

:TOMCAT_WAIT

timeout /t 2 /nobreak >nul

netstat -ano | findstr /R /C:":%TOMCAT_PORT% .*LISTENING" >nul 2>&1

if not errorlevel 1 (
    goto TOMCAT_READY
)

set /a TOMCAT_TRIES+=1

if !TOMCAT_TRIES! GEQ 30 (
    echo.
    echo ERROR: Tomcat failed to start within 60 seconds.
    echo Check the Tomcat window for errors.
    echo.
    pause
    exit /b 1
)

echo Waiting for Tomcat... !TOMCAT_TRIES!/30
goto TOMCAT_WAIT


:TOMCAT_READY

echo.
echo Tomcat is running.
echo.

REM Give Tomcat time to deploy the WAR

echo Waiting for Widgify deployment...

set /a DEPLOY_TRIES=0

:DEPLOY_WAIT

timeout /t 2 /nobreak >nul

if exist "%TOMCAT_APP%\WEB-INF\web.xml" (
    goto DEPLOY_READY
)

set /a DEPLOY_TRIES+=1

if !DEPLOY_TRIES! GEQ 30 (
    echo.
    echo WARNING: Could not verify WAR deployment.
    echo The desktop client will still be started.
    goto DEPLOY_READY
)

goto DEPLOY_WAIT


:DEPLOY_READY

echo Widgify server is ready.
echo.

REM ============================================================
REM [8/8] START DESKTOP CLIENT
REM ============================================================

echo [8/8] Starting Widgify Desktop Client...
echo.

start "Widgify - Desktop Client" cmd /k ^
    "cd /d ""%PROJECT_DIR%"" && call mvnw.cmd exec:java ""-Dexec.mainClass=com.widgify.desktop.WidgifyDesktopClient"" ""-Ddb.url=jdbc:mysql://localhost:3306/widgify?useSSL=false^&allowPublicKeyRetrieval=true^&serverTimezone=UTC"" ""-Ddb.user=root"" ""-Ddb.password="""

echo.
echo ============================================================
echo                    WIDGIFY IS READY
echo ============================================================
echo.
echo MySQL       : localhost:3306
echo Database    : widgify
echo Tomcat      : localhost:8080
echo Web App     : http://localhost:8080/widgify
echo Desktop     : JavaFX
echo.
echo Runtime files:
echo %RUNTIME_DIR%
echo.
echo Maven dependencies are downloaded automatically.
echo MySQL and Tomcat are downloaded automatically.
echo Database schema is initialized automatically.
echo.
echo ============================================================
echo.

timeout /t 5 /nobreak >nul

exit /b 0