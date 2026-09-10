#!/usr/bin/env sh
DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
if [ -f "$DIR/gradle/wrapper/gradle-wrapper.jar" ]; then
  exec java -classpath "$DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
fi
if command -v gradle >/dev/null 2>&1; then exec gradle "$@"; fi
echo "Gradle wrapper JAR is not bundled. Open the project in Android Studio, or install Gradle 8.13 and run gradle wrapper." >&2
exit 1
