# 架构与数据模型设计

## 1. 系统架构

```
┌─────────────┐      HTTPS/HTTP       ┌──────────────────────────┐
│  浏览器/同事  │ ───────────────────▶ │  Spring Boot (单进程)       │
│  (Vue SPA)   │                      │  :8080  (0.0.0.0)          │
└─────────────┘                      │  ┌────────┐  ┌─────────┐  │
                                     │  │ 前端静态│  │ REST API│  │
                                     │  │ static │  │/api/**  │  │
                                     │  └────────┘  └────┬────┘  │
                                     │                  │ JPA    │
                                     │            ┌─────▼─────┐  │
                                     │            │  MySQL    │  │
                                     │            │overtime_db│  │
                                     │            └───────────┘  │
                                     └──────────────────────────┘
```

- 前端构建产物由 Spring Boot 托管于 `/`（hash 路由，无需服务端路由回退）。
- API 统一前缀 `/api`，登录放行，其余经 `AuthInterceptor` 校验 `X-Auth-Token` 会话。
- 定时任务（`@Scheduled`）：每周一备份、每月10号透支提醒、12-31 节假日更新。

## 2. 模块 → 后端映射

| 设计文档模块 | 后端入口 |
| --- | --- |
| M0 登录鉴权 | `AuthController` + `AuthService` + `AuthInterceptor` |
| M1 成员管理 | `MemberController` / `MemberService` |
| M2 月度核算 | `OvertimeController` / `OvertimeService` / `ImportService` |
| M3 调休使用 | `CompUsageController` / `CompUsageService` |
| M4 剩余查询 | `BalanceController` / `BalanceService` |
| M5 员工门户 | `EmployeeController` |
| M6 统计首页 | `DashboardController` |
| M7 消息中心 | `MessageService` / `MessageController` |
| M8 系统设置 | `AccountController` / `ConfigController` / `BackupService` / `HolidayService` |
| M9 导入模板 | 前端下载；后端 `ImportService` 解析 |

## 3. 数据模型（10 张表）

| 表 | 实体 | 关键字段 |
| --- | --- | --- |
| `member` | `Member` | name, username(唯一), department, status |
| `overtime_record` | `OvertimeRecord` | member_id, overtime_date, type(工作日/周末/法定节假日), ratio, valid_hours, comp_hours, month |
| `comp_usage` | `CompUsage` | member_id, use_start, use_end, mode(1小时/2天数), hours, days, is_overdraft |
| `comp_adjustment` | `CompAdjustment` | member_id, date, category(项目奖励/领导奖励/公司福利/其他), hours |
| `sys_user` | `SysUser` | username, password_hash, role(ADMIN/CLERK/READONLY/EMPLOYEE), member_id, status, session_token, session_expire_at, must_change_pwd |
| `holiday_calendar` | `HolidayCalendar` | holiday_date(唯一), name, kind(HOLIDAY/WORKDAY), auto |
| `sys_message` | `SysMessage` | type, content, receiver_role, receiver_user, level, is_read |
| `audit_log` | `AuditLog` | user_id, username, action, detail |
| `backup_log` | `BackupLog` | filename, size, note |
| `sys_config` | `SysConfig` | conf_key, conf_value, description |

## 4. 核心业务规则

- **折算引擎**（`OvertimeService.buildRecord`）：优先用录入类型，否则按 `HolidayService.determineType` 判定（手动标记 > 节假日日历 > 周末/工作日默认）。
- **余额引擎**（`BalanceService.compute`）：`remaining = ΣcompHours + ΣadjustHours − ΣusageHours`，全两位小数；`remaining < 0` 即透支。
- **调休使用**（`CompUsageService`）：模式 A 直接计小时；模式 B/C 天数 × 标准工时（默认 7.5）折算小时；写入前与余额比较，不足则 `is_overdraft=1`。
- **精度**：所有金额/时长经 `NumberUtil.scale2`（HALF_UP）。
- **鉴权**：登录生成 UUID 会话 token 写入 `sys_user.session_token`（单设备：新登录覆盖旧 token）；拦截器校验 token 存在、未禁用、未过期。
- **密码**：BCrypt 哈希；强度正则 `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s]).{8,20}$`；不能与用户名相同。
- **导入校验**（`ImportService`）：先全量校验，全部通过才写入；任一行错误则返回错误清单（行号+原因），不落库。
- **备份**（`BackupService`）：优先 `mysqldump` 导出 `.sql` 压缩；无 mysqldump 时降级为全表 JSON 压缩；按 `backup_retention` 保留最新 N 份。
- **节假日**：`seedYear` 每年 12-31 由定时任务播种下一年（内置近似安排，需管理员年底校核）。

## 5. 角色权限

| 角色 | 可见模块 |
| --- | --- |
| ADMIN | 全部（含账号管理、参数、备份、节假日维护） |
| CLERK | 成员/核算/使用/其他调休/剩余/首页/消息/设置（除账号管理与参数） |
| READONLY | 只读查询类 |
| EMPLOYEE | 仅本人自助门户与消息 |
