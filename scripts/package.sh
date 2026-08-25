#!/usr/bin/env bash
# 构建前端并拷贝到后端静态目录（Git Bash / Linux）
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
echo "==> 构建前端"
cd "$ROOT/frontend"
npm install
npm run build

echo "==> 拷贝 dist -> backend/src/main/resources/static"
rm -rf "$ROOT/backend/src/main/resources/static"
mkdir -p "$ROOT/backend/src/main/resources/static"
cp -r dist/* "$ROOT/backend/src/main/resources/static/"

echo "==> 完成。下一步：cd backend && mvn package"
