@echo off
if exist "%~dp0gradle\wrapper\gradle-wrapper.jar" (
  java -classpath "%~dp0gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
) else (
  echo Gradle wrapper JAR is not bundled. Open the project in Android Studio or install Gradle 8.13.
  exit /b 1
)
