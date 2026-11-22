-- =========================================================================
-- Script khởi tạo toàn bộ Database cho hệ thống Microservices
-- Tự động thực thi khi Postgres container khởi động lần đầu
-- =========================================================================

-- 1. Database cho các dịch vụ QuickBite (theo docker-compose.yml & .env)
CREATE DATABASE quickbite_user_db;
CREATE DATABASE restaurant_db;
CREATE DATABASE order_db;
CREATE DATABASE notification_db;
CREATE DATABASE product_db;
CREATE DATABASE payment_db;

-- 2. Database hỗ trợ cấu hình StoreX (theo docker-compose.storex.yml)
CREATE DATABASE storex_user;
CREATE DATABASE storex_payment;
