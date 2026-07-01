#!/bin/sh
export DEFAULT_JVM_OPTS="-Xmx4g"
exec java $DEFAULT_JVM_OPTS -Dfile.encoding=UTF-8 -Duser.country=US -Duser.language=en -Duser.variant= -cp "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
