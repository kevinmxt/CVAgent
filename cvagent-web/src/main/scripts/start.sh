#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="${project.artifactId}-${project.version}.jar"
echo "CVAgent 启动中..."
exec java -Xmx512m -jar "$DIR/$JAR"
