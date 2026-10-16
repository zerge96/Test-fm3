@echo off
set DIR=%~dp0
set APP_HOME=%DIR%
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

if not exist "%CLASSPATH%" (
  echo Error: gradle-wrapper.jar not found at %CLASSPATH%
  echo Run: gradle wrapper --gradle-version 8.6 locally to generate the wrapper jar.
  exit /b 1
)

if defined JAVA_HOME (
  set JAVA_CMD=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_CMD=java
)

"%JAVA_CMD%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
