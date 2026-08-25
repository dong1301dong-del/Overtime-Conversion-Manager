# 加班转调休记录工具（LYNN）

内网 B/S 架构的加班转调休核算与查询平台，用于管理成员月度加班折算、调休使用、剩余查询与员工自助查询。依据《LYNN加班转调休记录工具》产品设计文档开发。

## 技术栈

| 层 | 选型 |
| --- | --- |
| 后端 | Spring Boot 3.2（Java 17）+ Spring Data JPA + Apache POI（Excel 导入） |
| 数据库 | MySQL 8.x |
| 前端 | Vue 3 + Vue Router + Element Plus + Axios |
| 构建 | Maven（后端）/ Vite（前端） |
| 部署 | 单体：前端构建产物由 Spring Boot 托管，单进程监听 8080 |

## 目录结构

```
D:\GitHub_Project
├── backend\                 # Spring Boot 后端（API + 内嵌前端静态资源）
│   ├── pom.xml
│   └── src\main\
│       ├── java\com\lynn\overtime\
│       │   ├── entity\        # JPA 实体（10 张表）
│       │   ├── repository\    # 数据访问
│       │   ├── service\       # 业务规则引擎
│       │   ├── controller\    # REST API
│       │   ├── common\        # 统一返回/鉴权/工具
│       │   ├── interceptor\   # 登录拦截器（单设备会话）
│       │   └── config\        # 启动种子数据
│       └── resources\
│           ├── application.yml
│           └── static\        # 前端构建产物（由脚本拷贝）
├── frontend\                # Vue3 前端源码
│   ├── src\views\           # 各角色页面
│   └── ...
├── docs\                    # 设计/部署文档
│   ├── ARCHITECTURE.md
│   └── DEPLOYMENT.md
└── scripts\                 # 构建/打包/运行脚本
```

## 默认账号

| 角色 | 用户名 | 初始密码 | 说明 |
| --- | --- | --- | --- |
| 管理员 | `admin` | `Admin@123456` | 首次登录强制改密 |
| 员工自助 | 随成员创建 | `Abc_123456` | 首次登录强制改密 |

## 仓库地址

- **GitHub**: [https://github.com/dong1301dong-del/Overtime-Conversion-Manager](https://github.com/dong1301dong-del/Overtime-Conversion-Manager)

## 版本与迭代日志

| 版本 | 状态 | 关键内容 |
| --- | --- | --- |
| v1.0.0 | ✅ 已发布 | 9 大模块（M0–M9）首次完整实现：单设备登录鉴权、成员/月加班/调休使用/余额/员工自助/统计/消息/系统设置/导入模板 |
| v1.0.1 | ✅ 已发布 | 鉴权拦截器漏判修复（`/api/auth/change-password`、`/api/auth/me` 拿不到登录态的根因）+ Navicat `caching_sha2_password` 兼容 + 四角色权限矩阵自动验收脚本 |

每次迭代的缺陷表、交付清单、部署动作、数据库影响、TODO 见 [docs/迭代日志.md](docs/迭代日志.md)。

## 四角色权限矩阵自动验收

`scripts/g6_probe.py` 是端到端的权限验证脚本，登录四个角色后跑遍所有受控端点：未登录 / 伪造 token / 角色越权 / 合法路径全部断言。结果：v1.0.1 实际 42/42 全 PASS。

```bash
python scripts/g6_probe.py
```

## 快速开始（开发）

```bash
# 后端（需先安装 JDK17 + Maven + MySQL，并建库 overtime_db）
cd backend && mvn spring-boot:run

# 前端（另开终端）
cd frontend && npm install && npm run dev   # http://localhost:5173
```

## 快速开始（生产单体部署）

详见 [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)。核心：构建前端 → 拷至 `backend/src/main/resources/static` → `mvn package` → 运行 jar（监听 `0.0.0.0:8080`），同局域网同事通过本机 IP 访问。

## 功能模块映射（对照设计文档）

- M0 登录鉴权：单设备登录、强制改密、密码强度
- M1 成员管理：增删改查、启用/禁用、自动创建员工自助账号
- M2 月度加班核算：手动录入 + Excel 导入（全量校验，错误行返回）
- M3 调休使用记录：模式 A（小时）/ B·C（天数），余额不足允许透支并标记
- M4 剩余调休查询：部门维度卡片 + 全员余额（负余额标红）
- M5 员工自助门户：本人余额、月度加班、使用记录，可导出/打印
- M6 统计详表首页：核心指标卡 + 历史月份跳转
- M7 消息中心：透支等重要消息提醒
- M8 系统设置：账号管理、节假日日历、数据备份、系统参数
- M9 导入模板：各模块模板下载

## 核心业务规则

- 折算：工作日 1:0.5、周末/法定节假日 1:1（天数模式 1 天 = 7.5 标准工时）
- 余额 = 累计产生 + 其他调休 − 累计使用，可为负（透支）
- 全系统两位小数精度
- 每月 10 号透支提醒；每年 12-31 自动更新下一年节假日
- 每周一自动备份，保留最近 10 份
