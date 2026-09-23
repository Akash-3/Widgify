@REM ----------------------------------------------------------------------------
@REM Maven Wrapper for Widgify Project
@REM ----------------------------------------------------------------------------
@echo off
setlocal

@REM Ensure JDK 17 is preferred over Android Studio JBR if available
if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
)

set "MAVEN_HOME=C:\Users\tempm\.m2\apache-maven-3.9.6"
if exist "%MAVEN_HOME%\bin\mvn.cmd" (
    call "%MAVEN_HOME%\bin\mvn.cmd" %*
) else (
    echo Error: Maven binary not found at %MAVEN_HOME%
    exit /b 1
)
