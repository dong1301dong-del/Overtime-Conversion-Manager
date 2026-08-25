@echo off
REM 构建前端并拷贝到后端静态目录（Windows 双击运行）
cd /d %~dp0..
cd frontend
call npm install
call npm run build
cd ..
if not exist backend\src\main\resources\static mkdir backend\src\main\resources\static
xcopy /E /Y /I frontend\dist\* backend\src\main\resources\static\
echo 完成。下一步：cd backend 后执行 mvn package
pause
