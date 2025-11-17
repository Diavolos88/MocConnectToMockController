@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-17
set M2_HOME=C:\Program Files\Java\apache-maven-3.9.11

@REM echo Останавливаю сервис на порту 8081...
@REM for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8081 ^| findstr LISTENING') do (
@REM     echo Останавливаю процесс PID: %%a
@REM     taskkill /F /PID %%a >nul 2>&1
@REM )
@REM timeout /t 2 /nobreak >nul
@REM echo.
echo Запускаю сервис заново...
"%M2_HOME%\bin\mvn.cmd" spring-boot:run

