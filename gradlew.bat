@echo off
set DEFAULT_JVM_OPTS=-Xmx4g
java %DEFAULT_JVM_OPTS% -Dfile.encoding=UTF-8 -Duser.country=US -Duser.language=en -Duser.variant= -cp "%~dp0\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
