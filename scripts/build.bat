@echo off
REM 构建并打包前后端为单一 jar（G3）
cd /d %~dp0\..
REM 自动定位 JDK
if not defined JAVA_HOME (
  for /d %%d in ("C:\Program Files\Microsoft\jdk-*") do set JAVA_HOME=%%d
  if not defined JAVA_HOME for /d %%d in ("C:\Program Files\Java\jdk-*") do set JAVA_HOME=%%d
)
set PATH=%JAVA_HOME%\bin;%PATH%
REM 自动定位 Maven（优先 D:\tools 免安装版）
set MAVEN=D:\tools\apache-maven-3.9.9\bin\mvn.cmd
if not exist "%MAVEN%" for /d %%d in ("D:\tools\apache-maven-*") do set MAVEN=%%d\bin\mvn.cmd
if not exist "%MAVEN%" for /d %%d in ("C:\Program Files\apache-maven-*") do set MAVEN=%%d\bin\mvn.cmd
echo JAVA_HOME=%JAVA_HOME%
echo MAVEN=%MAVEN%
echo === 1/2 构建前端 ===
cd frontend
call npm install
call npm run build
cd ..
echo === 2/2 拷贝前端到后端静态目录并打包 ===
if not exist backend\src\main\resources\static mkdir backend\src\main\resources\static
xcopy /E /Y /I frontend\dist\* backend\src\main\resources\static\
cd backend
call "%MAVEN%" -q clean package -DskipTests
echo 打包完成：backend\target\overtime-comp-leave.jar
pause
