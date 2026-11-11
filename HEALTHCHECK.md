# Cơ Chế Health Check Trong Dự Án Storex

Tài liệu này giải thích ngắn gọn về cơ chế **Health Check** (kiểm tra trạng thái sẵn sàng) được thiết lập trong dự án `storex` thông qua Docker Compose.

---

## 1. Vấn Đề Cần Giải Quyết (Race Condition)

Theo mặc định, lệnh `depends_on` trong Docker Compose chỉ đợi container phụ thuộc **khởi chạy (started)**, chứ **không đợi** dịch vụ bên trong (PostgreSQL, Redis) sẵn sàng nhận kết nối mạng:
- Khi PostgreSQL hoặc Redis vừa khởi động, chúng mất vài giây để đọc dữ liệu và mở port socket.
- Nếu ứng dụng Spring Boot (`backend`) khởi động cùng lúc, nó sẽ cố gắng kết nối ngay lập tức qua HikariCP (Postgres) và Lettuce (Redis).
- Hậu quả: Gặp lỗi `Connection Refused`, ứng dụng Spring Boot bị crash ngay khi khởi động.

---

## 2. Giải Pháp: Health Check & `service_healthy`

Dự án áp dụng cơ chế **Health Check** kết hợp `condition: service_healthy` trong [`docker-compose.yml`](docker-compose.yml):

```yaml
services:
  backend:
    image: storex-backend:latest
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
```

Backend sẽ **chờ cho đến khi cả hai dịch vụ báo trạng thái `healthy`** thì mới bắt đầu khởi chạy.

---

## 3. Chi Tiết Cấu Hình Từng Dịch Vụ

### A. PostgreSQL Health Check
```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U postgres -d storex"]
  interval: 3s
  timeout: 5s
  retries: 5
```
- **Lệnh kiểm tra (`test`)**: Dùng công cụ `pg_isready` tích hợp sẵn trong PostgreSQL để kiểm tra xem database `storex` và user `postgres` đã sẵn sàng nhận kết nối hay chưa.
- **Tần suất (`interval: 3s`)**: Cứ mỗi 3 giây thực hiện kiểm tra 1 lần.
- **Thời gian chờ (`timeout: 5s`)**: Mỗi lần kiểm tra tối đa 5 giây.
- **Số lần thử lại (`retries: 5`)**: Thử lại tối đa 5 lần trước khi đánh dấu container bị lỗi (`unhealthy`).

---

### B. Redis Health Check
```yaml
healthcheck:
  test: ["CMD", "redis-cli", "ping"]
  interval: 3s
  timeout: 5s
  retries: 5
```
- **Lệnh kiểm tra (`test`)**: Dùng `redis-cli ping`. Khi Redis đã sẵn sàng xử lý request, nó sẽ phản hồi `PONG` (mã thoát 0).
- **Tần suất & Thời gian chờ**: Chạy mỗi 3 giây, timeout 5 giây, thử lại tối đa 5 lần.

---

## 4. Cách Kiểm Tra Trạng Thái Health Check

Bạn có thể kiểm tra trạng thái sức khỏe của các container bất kỳ lúc nào bằng lệnh:

```bash
docker compose ps
```

**Kết quả mẫu:**
```
NAME                IMAGE                   STATUS                    PORTS
storex-backend-1    storex-backend:latest   Up 2 minutes              0.0.0.0:8080->8080/tcp
storex-postgres-1   postgres:13             Up 2 minutes (healthy)    0.0.0.0:5432->5432/tcp
storex-redis-1      redis:7-alpine          Up 2 minutes (healthy)    0.0.0.0:6379->6379/tcp
```
> Trạng thái `(healthy)` xác nhận dịch vụ đã hoàn toàn sẵn sàng và ổn định.
