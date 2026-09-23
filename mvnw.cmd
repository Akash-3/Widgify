@REM ----------------------------------------------------------------------------
@REM Maven Wrapper for Widgify Project
@REM ----------------------------------------------------------------------------
@echo off
setlocal EnableExtensions

set "PROJECT_DIR=%~dp0"
set "MAVEN_VERSION=3.9.11"
set "MAVEN_HOME=%PROJECT_DIR%.widgify\maven\apache-maven-%MAVEN_VERSION%"
set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"

where mvn >nul 2>&1
if not errorlevel 1 (
    for /f "delims=" %%M in ('where mvn') do (
        set "MAVEN_CMD=%%M"
        goto RUN_MAVEN
    )
)

if exist "%MAVEN_CMD%" goto RUN_MAVEN

where powershell >nul 2>&1
if errorlevel 1 (
    echo Error: Maven is not installed and PowerShell was not found.
    echo Install Maven or Windows PowerShell 5.1 and try again.
    exit /b 1
)

if not exist "%PROJECT_DIR%.widgify\maven" mkdir "%PROJECT_DIR%.widgify\maven"
set "MAVEN_ZIP=%PROJECT_DIR%.widgify\maven.zip"
echo Downloading Apache Maven %MAVEN_VERSION% for this project...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ProgressPreference='SilentlyContinue'; try { Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%MAVEN_ZIP%' -UseBasicParsing -ErrorAction Stop; Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%PROJECT_DIR%.widgify\maven' -Force; exit 0 } catch { Write-Error $_; exit 1 }"
if errorlevel 1 (
    echo Error: Unable to download Apache Maven.
    if exist "%MAVEN_ZIP%" del /f /q "%MAVEN_ZIP%" >nul 2>&1
    exit /b 1
)
del /f /q "%MAVEN_ZIP%" >nul 2>&1

if not exist "%MAVEN_CMD%" (
    echo Error: Maven installation is incomplete.
    exit /b 1
)

:RUN_MAVEN
call "%MAVEN_CMD%" %*
exit /b %errorlevel%
