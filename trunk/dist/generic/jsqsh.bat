@echo off

rem       I hate windows. I have no idea why this works.
rem       Glory be to the Googles for finding an example.
setLocal EnableDelayedExpansion
set CLASSPATH="
for /R ..\share %%j in (*.jar) do (
    set CLASSPATH=%%j;!CLASSPATH!
)
SET CLASSPATH=!CLASSPATH!"

java org.sqsh.JSqsh %*
