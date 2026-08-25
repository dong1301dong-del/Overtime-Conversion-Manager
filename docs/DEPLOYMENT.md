# 部署上线指南（LYNN 笔记本 · 内网单机）

本应用为单体部署：前端构建后由 Spring Boot 托管，单进程监听 `0.0.0.0:8080`，同局域网同事通过本机 IP 访问。

> ⚠️ 上线每一步均先汇报、确认放行后再执行（见下文「部署闸门」）。

## 一、环境前置（仅首次）

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| JDK | 17 | 运行/构建后端 |
| Maven | 3.9+ | 构建后端 |
| MySQL | 8.x | 数据库（需建库 `overtime_db`） |
| Node.js | 22 | 构建前端 |

### 1. 安装（winget 可一行完成）
```powershell
winget install Microsoft.OpenJDK.17
winget install Apache.Maven
winget install Oracle.MySQL
winget install OpenJS.NodeJS.LTS
```
安装后请**重启终端**使 `java / mvn / mysql / node` 进入 PATH。

### 2. 创建数据库
```sql
-- 用 MySQL root 执行
CREATE DATABASE IF NOT EXISTS overtime_db
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```
应用默认以 `root / root` 连接；若你的 MySQL root 密码不同，启动前设置环境变量：
```powershell
$env:DB_USERNAME="root"; $env:DB_PASSWORD="你的密码"
```
（或首次启动时我帮你建专用账号并设置。）

## 二、构建与部署闸门（逐步确认）

部署分以下闸门，每步执行前我会先说明并等你确认：

1. **G1 安装环境**（JDK / Maven / MySQL / Node）——已装可跳过。
2. **G2 建库** `overtime_db`。
3. **G3 构建前端**：`npm install` → `npm run build` → 拷贝 `dist` 到 `backend/src/main/resources/static`。
4. **G4 打包后端**：`mvn package` 生成 `overtime-comp-leave.jar`。
5. **G5 启动服务**：`java -jar backend/target/overtime-comp-leave.jar`，绑定 `0.0.0.0:8080`。
6. **G6 开机自启**（可选）：用 Windows 任务计划程序或 `nssm` 注册服务。
7. **G7 验收**：用四角色账号登录，走通录入/导入/查询/备份。

> 一键脚本见 `scripts/package.sh`（Git Bash）或 `scripts/package.bat`（双击）。

## 三、运行与访问

- 本机访问：<http://localhost:8080>
- 同事访问：<http://你的内网IP:8080>（防火墙需放行 8080）
- 管理后台默认账号：`admin / Admin@123456`（首次登录强制改密）

## 四、配置说明

通过环境变量覆盖（无需改代码）：

| 变量 | 默认 | 说明 |
| --- | --- | --- |
| `DB_USERNAME` / `DB_PASSWORD` | root / root | 数据库连接 |
| `APP_DATA_DIR` | `./data` | 运行数据/日志目录 |
| `APP_BACKUP_DIR` | `./backups` | 备份目录 |
| `ADMIN_PASSWORD` | `Admin@123456` | 首次种子管理员密码（仅当 admin 不存在时） |

系统参数（标准工时、精度、备份保留、会话超时）可在「系统设置 → 系统参数」中改，存于 `sys_config`，默认：标准工时 `7.5`、精度 `2`、保留 `10` 份、会话 `24h`。

## 五、备份与恢复

- **自动**：每周一 03:30 全量备份，保留最近 10 份（可在「系统设置 → 数据备份」手动触发）。
- **方式**：优先 `mysqldump` 导出 `.sql` 压缩；若环境无 `mysqldump` 则降级为全表 JSON 压缩。
- **恢复**：优先用 `mysql overtime_db < backup_xxx.sql`；JSON 备份用于审计/导出。

## 六、目录与版本管理

- 代码在 `D:\GitHub_Project`，已初始化 git，迭代持续 commit。
- 运行时数据（`backend/data`、`backend/backups`、前端 `dist`）已加入 `.gitignore`，不进版本库。
