#!/bin/bash

# Linux启动脚本
JAR_NAME="file-note-0.1.jar"
LOG_FILE="out.log"

# 检查jar文件是否存在
if [ ! -f "$JAR_NAME" ]; then
    echo "错误：找不到$JAR_NAME文件"
    exit 1
fi

# 启动应用
nohup java -jar "$JAR_NAME" > "$LOG_FILE" 2>&1 &

echo "应用已启动，日志输出到$LOG_FILE"
echo "进程ID：$!"