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

set "MYSQL_VERSION=8.0.46"
set "MYSQL_DIR=%RUNTIME_DIR%\mysql"
set "MYSQL_DATA=%MYSQL_DIR%\data"
set "MYSQL_BIN=%MYSQL_DIR%\bin"
set "MYSQL_EXE=%MYSQL_BIN%\mysqld.exe"
set "MYSQL_CLIENT=%MYSQL_BIN%\mysql.exe"
set "MYSQL_ADMIN=%MYSQL_BIN%\mysqladmin.exe"
set "MYSQL_ERROR_LOG=%MYSQL_DATA%\widgify-mysql.err"

set "MYSQL_PORT=3306"
set "MYSQL_DATABASE=widgify"
set "MYSQL_USER=root"
set "MYSQL_PASSWORD="

set "TOMCAT_VERSION=10.1.20"
set "TOMCAT_DIR=%RUNTIME_DIR%\tomcat"

set "JDK_VERSION=17.0.20.1"
set "JDK_DIR=%RUNTIME_DIR%\jdk"
set "JDK_ZIP=%RUNTIME_DIR%\jdk.zip"

set "TOMCAT_PORT=8080"
set "TOMCAT_WEBAPPS=%TOMCAT_DIR%\webapps"

set "WAR_FILE=%PROJECT_DIR%target\widgify.war"
set "TOMCAT_WAR=%TOMCAT_WEBAPPS%\widgify.war"
set "TOMCAT_APP=%TOMCAT_WEBAPPS%\widgify"

cd /d "%PROJECT_DIR%"

if /i "%~1"=="--check" goto CHECK_ONLY

if not exist "%PROJECT_DIR%database\schema.sql" (
    echo ERROR: database\schema.sql was not found.
    echo Run this launcher from the complete Widgify project folder.
    exit /b 1
)

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

set "JAVA_HOME="

if exist "%JDK_DIR%\bin\java.exe" (
    set "JAVA_HOME=%JDK_DIR%"
)

if not defined JAVA_HOME (
    for /d %%D in (
        "C:\Program Files\Java\*"
        "C:\Program Files\Eclipse Adoptium\*"
        "C:\Program Files\Microsoft\*"
        "C:\Program Files\Android\Android Studio\jbr"
    ) do (
        if exist "%%~D\bin\java.exe" (
            set "JAVA_HOME=%%~D"
            goto JAVA_HOME_FOUND
        )
    )
)

:JAVA_HOME_FOUND

if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

where java >nul 2>&1

if errorlevel 1 (
    echo Java was not found in PATH or common install folders.
    echo Downloading a portable JDK %JDK_VERSION% for Widgify...

    where powershell >nul 2>&1
    if errorlevel 1 (
        echo.
        echo ERROR: Windows PowerShell was not found.
        echo Widgify uses PowerShell to extract the bundled JDK.
        echo.
        pause
        exit /b 1
    )

    where curl.exe >nul 2>&1
    if errorlevel 1 (
        echo.
        echo ERROR: curl.exe was not found.
        echo Widgify needs curl.exe to download the JDK automatically.
        echo.
        pause
        exit /b 1
    )

    if exist "%JDK_DIR%" (
        rmdir /s /q "%JDK_DIR%"
    )
    if exist "%JDK_ZIP%" (
        del /f /q "%JDK_ZIP%" >nul 2>&1
    )

    curl.exe --fail --location --retry 3 --retry-delay 2 --output "%JDK_ZIP%" ^
        "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse"

    if errorlevel 1 (
        echo.
        echo ERROR: JDK download failed.
        echo Widgify could not download a Java runtime automatically.
        echo.
        pause
        exit /b 1
    )

    if not exist "%JDK_ZIP%" (
        echo.
        echo ERROR: JDK archive was not created.
        echo.
        pause
        exit /b 1
    )

    if exist "%RUNTIME_DIR%\jdk_extract" (
        rmdir /s /q "%RUNTIME_DIR%\jdk_extract"
    )

    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "try { Expand-Archive -Path '%JDK_ZIP%' -DestinationPath '%RUNTIME_DIR%\jdk_extract' -Force -ErrorAction Stop } catch { exit 1 }"

    if errorlevel 1 (
        echo.
        echo ERROR: JDK extraction failed.
        echo.
        pause
        exit /b 1
    )

    set "JAVA_HOME="
    for /d %%D in ("%RUNTIME_DIR%\jdk_extract\*") do (
        if exist "%%~D\bin\java.exe" (
            set "JAVA_HOME=%%~D"
            goto JDK_READY
        )
    )

    :JDK_READY
    if not defined JAVA_HOME (
        echo.
        echo ERROR: JDK installation is incomplete.
        echo.
        pause
        exit /b 1
    )

    if exist "%JDK_DIR%" (
        rmdir /s /q "%JDK_DIR%"
    )
    move /y "%JAVA_HOME%" "%JDK_DIR%" >nul
    set "JAVA_HOME=%JDK_DIR%"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    del /f /q "%JDK_ZIP%" >nul 2>&1
    rmdir /s /q "%RUNTIME_DIR%\jdk_extract"
)

where powershell >nul 2>&1

if errorlevel 1 (
    echo.
    echo ERROR: Windows PowerShell was not found.
    echo Widgify uses PowerShell to download and unpack its local runtime.
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

set "MYSQL_ZIP=%RUNTIME_DIR%\mysql.zip"

REM ------------------------------------------------------------
REM Download MySQL
REM ------------------------------------------------------------

echo Downloading MySQL %MYSQL_VERSION%...
echo.

where curl.exe >nul 2>&1
if errorlevel 1 (
    echo.
    echo ERROR: curl.exe was not found.
    echo Install Windows 10 or newer, or install curl and add it to PATH.
    echo.
    pause
    exit /b 1
)

curl.exe --fail --location --retry 3 --retry-delay 2 --output "%MYSQL_ZIP%" ^
    "https://cdn.mysql.com/Downloads/MySQL-8.0/mysql-8.0.46-winx64.zip"
if errorlevel 1 (
    echo.
    echo ERROR: MySQL download failed.
    echo.
    echo The MySQL download server did not provide the ZIP file.
    echo.
    if exist "%MYSQL_ZIP%" del /f /q "%MYSQL_ZIP%" >nul 2>&1
    pause
    exit /b 1
)

REM ------------------------------------------------------------
REM Verify downloaded file
REM ------------------------------------------------------------

echo Verifying MySQL download...

if not exist "%MYSQL_ZIP%" (
    echo.
    echo ERROR: MySQL ZIP file was not created.
    echo.
    pause
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "try { Add-Type -AssemblyName System.IO.Compression.FileSystem; [System.IO.Compression.ZipFile]::OpenRead('%MYSQL_ZIP%').Dispose(); exit 0 } catch { exit 1 }"

if errorlevel 1 (
    echo.
    echo ERROR: Downloaded MySQL file is not a valid ZIP archive.
    echo.
    echo The download server may have returned an error page instead.
    echo.
    del /f /q "%MYSQL_ZIP%" >nul 2>&1
    pause
    exit /b 1
)

echo MySQL ZIP verified successfully.
echo.

REM ------------------------------------------------------------
REM Extract MySQL
REM ------------------------------------------------------------

echo Extracting MySQL...

if exist "%RUNTIME_DIR%\mysql_extract" (
    rmdir /s /q "%RUNTIME_DIR%\mysql_extract"
)

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "try { Expand-Archive -Path '%MYSQL_ZIP%' -DestinationPath '%RUNTIME_DIR%\mysql_extract' -Force -ErrorAction Stop } catch { exit 1 }"

if errorlevel 1 (
    echo.
    echo ERROR: Failed to extract MySQL.
    echo.
    pause
    exit /b 1
)

REM ------------------------------------------------------------
REM Move extracted MySQL directory
REM ------------------------------------------------------------

if exist "%MYSQL_DIR%" (
    rmdir /s /q "%MYSQL_DIR%"
)

for /d %%D in ("%RUNTIME_DIR%\mysql_extract\mysql-*") do (
    move "%%D" "%MYSQL_DIR%" >nul
)

rmdir /s /q "%RUNTIME_DIR%\mysql_extract"
del /f /q "%MYSQL_ZIP%" >nul 2>&1

REM ------------------------------------------------------------
REM Verify MySQL installation
REM ------------------------------------------------------------

if not exist "%MYSQL_EXE%" (
    echo.
    echo ERROR: MySQL installation is incomplete.
    echo mysqld.exe was not found.
    echo.
    pause
    exit /b 1
)

if not exist "%MYSQL_CLIENT%" (
    echo.
    echo ERROR: MySQL installation is incomplete.
    echo mysql.exe was not found.
    echo.
    pause
    exit /b 1
)

:MYSQL_INSTALLED

echo MySQL is available.
echo.

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

REM A listening port alone is not enough: another application may own 3306.
if exist "%MYSQL_ADMIN%" (
    "%MYSQL_ADMIN%" --host=127.0.0.1 --port=%MYSQL_PORT% --user=%MYSQL_USER% ping >nul 2>&1
    if not errorlevel 1 (
        echo Portable MySQL is already running and responding.
        goto MYSQL_READY
    )
)

netstat -ano | findstr /R /C:":%MYSQL_PORT% .*LISTENING" >nul 2>&1
if not errorlevel 1 (
    echo.
    echo ERROR: Port %MYSQL_PORT% is already used by another process.
    echo Widgify cannot start its MySQL server on this PC.
    echo.
    for /f "tokens=5" %%P in ('netstat -ano ^| findstr /R /C:":%MYSQL_PORT% .*LISTENING"') do (
        echo Port owner PID: %%P
        tasklist /FI "PID eq %%P" /FO LIST | findstr /I "Image Name PID" 
    )
    echo.
    echo Close the other MySQL/application using port %MYSQL_PORT%, then run Widgify again.
    pause
    exit /b 1
)

echo Starting portable MySQL...

start "Widgify - MySQL" /min ^
    "%MYSQL_EXE%" ^
    --console ^
    --basedir="%MYSQL_DIR%" ^
    --datadir="%MYSQL_DATA%" ^
    --port=%MYSQL_PORT% ^
    --bind-address=127.0.0.1 ^
    --log-error="%MYSQL_ERROR_LOG%"

set /a MYSQL_TRIES=0

:MYSQL_WAIT

timeout /t 2 /nobreak >nul

if exist "%MYSQL_ADMIN%" (
    "%MYSQL_ADMIN%" --host=127.0.0.1 --port=%MYSQL_PORT% --user=%MYSQL_USER% ping >nul 2>&1
    if not errorlevel 1 goto MYSQL_READY
)

set /a MYSQL_TRIES+=1

if !MYSQL_TRIES! GEQ 30 (
    echo.
    echo ERROR: MySQL failed to become ready within 60 seconds.
    echo.
    echo MySQL error log: %MYSQL_ERROR_LOG%
    if exist "%MYSQL_ERROR_LOG%" (
        echo -------- Last MySQL errors --------
        powershell -NoProfile -Command "Get-Content -LiteralPath '%MYSQL_ERROR_LOG%' -Tail 25"
        echo -----------------------------------
    )
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
REM [6/9] CLEAN BUILD WIDGIFY
REM ============================================================

echo [6/9] Removing stale build output...

if exist "%PROJECT_DIR%target" (
    rmdir /s /q "%PROJECT_DIR%target"
)

if exist "%PROJECT_DIR%target" (
    echo.
    echo ERROR: Could not remove the old target directory.
    echo Close any process using the project and try again.
    pause
    exit /b 1
)

echo Old build output removed.
echo.
echo Building Widgify from the current source files...
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
REM [7/9] DEPLOY AND START TOMCAT
REM ============================================================

echo [7/9] Deploying Widgify server...

set "CATALINA_HOME=%TOMCAT_DIR%"
set "CATALINA_BASE=%TOMCAT_DIR%"

REM Stop existing portable Tomcat

netstat -ano | findstr /R /C:":%TOMCAT_PORT% .*LISTENING" >nul 2>&1

if not errorlevel 1 (
    echo Existing Tomcat detected.
    echo Stopping it...

    call "%TOMCAT_DIR%\bin\shutdown.bat" >nul 2>&1

    set /a STOP_TRIES=0

:TOMCAT_STOP_WAIT
    timeout /t 1 /nobreak >nul
    netstat -ano | findstr /R /C:":%TOMCAT_PORT% .*LISTENING" >nul 2>&1
    if errorlevel 1 goto TOMCAT_STOPPED
    set /a STOP_TRIES+=1
    if !STOP_TRIES! GEQ 15 (
        echo Tomcat did not stop cleanly. Stopping the process on port %TOMCAT_PORT%...
        for /f "tokens=5" %%P in ('netstat -ano ^| findstr /R /C:":%TOMCAT_PORT% .*LISTENING"') do taskkill /PID %%P /F >nul 2>&1
        timeout /t 2 /nobreak >nul
        goto TOMCAT_STOPPED
    )
    goto TOMCAT_STOP_WAIT
)

:TOMCAT_STOPPED

REM Remove old exploded deployment and Tomcat work/temp caches.

if exist "%TOMCAT_APP%" (
    rmdir /s /q "%TOMCAT_APP%"
)

if exist "%TOMCAT_WAR%" (
    del /f /q "%TOMCAT_WAR%"
)

if exist "%TOMCAT_DIR%\work" (
    rmdir /s /q "%TOMCAT_DIR%\work"
)

if exist "%TOMCAT_DIR%\temp" (
    rmdir /s /q "%TOMCAT_DIR%\temp"
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

REM Prefer an HTTP readiness check as well; Tomcat can serve the app before
REM the exploded directory is visible on slower disks or OneDrive folders.
curl.exe --fail --silent --show-error --max-time 5 "http://127.0.0.1:%TOMCAT_PORT%/widgify/login.jsp" >nul 2>&1
if not errorlevel 1 (
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
REM [8/9] VERIFY LOGIN AGAINST THE FRESH DEPLOYMENT
REM ============================================================

echo [8/9] Running authentication smoke test...
echo This registers a temporary user, verifies correct login, rejects bad credentials,
echo and verifies the authenticated session can load widgets.
echo.

call "%PROJECT_DIR%mvnw.cmd" -q exec:java "-Dexec.mainClass=com.widgify.desktop.net.ServerApiClientTest" "-Dexec.jvmArgs=-ea"

if errorlevel 1 (
    echo.
    echo ============================================================
    echo                 LOGIN SMOKE TEST FAILED
    echo ============================================================
    echo.
    echo The fresh WAR was deployed, but authentication did not pass.
    echo The desktop client will not be started.
    echo.
    pause
    exit /b 1
)

echo Authentication smoke test passed.
echo.

REM ============================================================
REM [9/9] START DESKTOP CLIENT
REM ============================================================

echo [9/9] Starting Widgify Desktop Client...
echo.

start "Widgify - Desktop Client" /D "%PROJECT_DIR%" cmd /k ^
    call "%PROJECT_DIR%mvnw.cmd" exec:java "-Dexec.mainClass=com.widgify.desktop.WidgifyDesktopClient" "-Ddb.url=jdbc:mysql://localhost:3306/widgify?useSSL=false" "-Ddb.user=root" "-Ddb.password="

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

:CHECK_ONLY

echo Checking Widgify launcher prerequisites only...

if not exist "%PROJECT_DIR%database\schema.sql" (
    echo ERROR: database\schema.sql was not found.
    exit /b 1
)

where java >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java was not found on PATH.
    exit /b 1
)

where powershell >nul 2>&1
if errorlevel 1 (
    echo ERROR: Windows PowerShell was not found.
    exit /b 1
)

call "%PROJECT_DIR%mvnw.cmd" -version
if errorlevel 1 (
    echo ERROR: The portable Maven wrapper could not start.
    exit /b 1
)

echo.
echo Launcher prerequisite check passed.
exit /b 0