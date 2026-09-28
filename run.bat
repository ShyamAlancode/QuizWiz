@echo off
title QuizWiz - Online Quiz System
cd /d "%~dp0"

echo ================================================
echo Starting QuizWiz System...
echo ================================================

rem Free port 8080 if an old instance is running
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8080" ^| findstr "LISTENING"') do (
    echo Freeing port 8080 - closing PID %%a
    taskkill /F /PID %%a >nul 2>&1
)

set "JAVA_HOME=C:\Program Files\Java\jdk-21"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] JDK 21 not found at %JAVA_HOME%
    echo Please make sure Java 21 is installed.
    pause
    exit /b 1
)

if exist "target\quizwiz-0.0.1-SNAPSHOT.jar" (
    echo Running packaged application...
    "%JAVA_HOME%\bin\java.exe" -jar "target\quizwiz-0.0.1-SNAPSHOT.jar"
) else (
    echo Compiling and running with Maven...
    call mvnw.cmd spring-boot:run
)

pause
