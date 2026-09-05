@echo off
cd /d "%~dp0"
title Minecraft Server 1.21
color 0b

echo ====================================================================
echo                 MINECRAFT SERVER 1.21.4 - PAPER
echo ====================================================================
echo.

set "JAVA_EXE=jdk-21\bin\java.exe"
if exist "%JAVA_EXE%" goto :run
set "JAVA_EXE=java"

:run
echo Starting Minecraft Server...
echo To stop server safely, type: stop
echo ====================================================================
echo.

"%JAVA_EXE%" -Xms2G -Xmx4G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true -jar server.jar nogui

echo.
echo Server stopped.
pause
