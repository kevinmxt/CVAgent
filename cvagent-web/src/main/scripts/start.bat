@echo off
chcp 65001 >nul
title CVAgent
echo CVAgent 启动中...
java -Xmx512m -jar "%~dp0${project.artifactId}-${project.version}.jar"
pause
