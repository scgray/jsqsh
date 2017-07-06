@echo off
java  -Dlog4j.configuration=file://../conf/log4j.properties -cp "..\share\jsqsh\*" org.sqsh.JSqsh %*
