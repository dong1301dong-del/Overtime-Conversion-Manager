-- 便携版 MySQL 初始化后执行：设置 root 口令为 root，并创建业务库
-- 用法（root 初始为空口令）：
--   D:\tools\mysql\bin\mysql -u root --skip-password < scripts/mysql-init.sql
ALTER USER 'root'@'localhost' IDENTIFIED BY 'root';
CREATE DATABASE IF NOT EXISTS overtime_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
FLUSH PRIVILEGES;
