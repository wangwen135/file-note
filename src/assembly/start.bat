@echo off

:: Windows启动脚本
set JAR_NAME=file-note-0.1.1.jar
set LOG_FILE=out.log

:: 检查jar文件是否存在
if not exist "%JAR_NAME%" (
    echo 错误：找不到%JAR_NAME%文件
    pause
    exit /b 1
)

:: 启动应用（使用start命令在后台运行）
start "File Note Application" java -jar "%JAR_NAME%" > "%LOG_FILE%" 2>&1

echo 应用已启动，日志输出到%LOG_FILE%
echo 按任意键退出...
pause > nul