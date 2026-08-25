@echo off
REM 启动加班转调休记录工具（需先完成 G3 打包生成 backend\target\overtime-comp-leave.jar）
REM 支持：登录自启（启动文件夹调用）、崩溃/端口冲突自愈
cd /d %~dp0\..

REM 自动探测 JDK17
if not defined JAVA_HOME (
  for /d %%d in ("C:\Program Files\Microsoft\jdk-*") do set JAVA_HOME=%%d
  if not defined JAVA_HOME for /d %%d in ("C:\Program Files\Java\jdk-*") do set JAVA_HOME=%%d
)
set PATH=%JAVA_HOME%\bin;%PATH%
echo JAVA_HOME=%JAVA_HOME%

:loop
REM 端口已占用（已有实例在跑）则跳过，避免重复启动冲突
netstat -an 2>nul | findstr ":8080" | findstr "LISTEN" >nul
if %errorlevel%==0 (
  echo [%date% %time%] 8080 已被占用，等待中...
  timeout /t 15 /nobreak >nul
  goto loop
)
echo [%date% %time%] 正在启动服务，监听 http://0.0.0.0:8080 ...
java -Dserver.port=8080 -jar backend\target\overtime-comp-leave.jar
echo [%date% %time%] 服务已退出（退出码 %errorlevel%），5 秒后自动重启...
timeout /t 5 /nobreak >nul
goto loop
