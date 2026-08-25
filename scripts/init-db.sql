-- 初始化数据库（使用 MySQL root 执行一次）
CREATE DATABASE IF NOT EXISTS overtime_db
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

-- 可选：创建专用账号（如不使用 root/root）
-- CREATE USER IF NOT EXISTS 'overtime'@'localhost' IDENTIFIED BY 'Overtime@123456';
-- GRANT ALL PRIVILEGES ON overtime_db.* TO 'overtime'@'localhost';
-- FLUSH PRIVILEGES;
