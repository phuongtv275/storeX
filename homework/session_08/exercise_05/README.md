# Báo Cáo Phân Tích Kỹ Thuật: Nguyên Nhân Tăng Thời Gian Build Khi Chuyển Sang Multi-stage Dockerfile

**Kính gửi:** Giám đốc Kỹ thuật (CTO) & Đội ngũ Kỹ thuật QuickBite  
**Dịch vụ:** `order-service`  
**Vấn đề:** Thời gian pipeline CI/CD tăng từ ~1.5 phút lên 4-5 phút; Gradle phải tải lại toàn bộ dependencies dù đã cấu hình chia sẻ Docker socket (DooD) trên Self-hosted Runner.

---

### 1. Nguyên Nhân Gốc Rễ (Root Cause)

Sự sụt giảm hiệu năng xuất phát từ **tính cô lập tuyệt đối về hệ thống tệp (Filesystem Isolation)** của tiến trình biên dịch bên trong Docker container trong kiến trúc Multi-stage build.

### 2. Sự Khác Biệt Giữa Hai Mô Hình Thực Thi

- **Mô hình cũ (Thực thi trực tiếp trên Host Runner):**  
  Lệnh `./gradlew build` được thực thi trực tiếp trên hệ điều hành của Self-hosted Runner. Tiến trình này kế thừa toàn bộ filesystem của host và tái sử dụng thư mục cache `~/.gradle/caches` đã tồn tại từ các lần build trước. Gradle chỉ cần đọc thư viện từ ổ cứng cục bộ mà không cần tải lại qua internet, giúp hoàn thành chỉ trong chưa tới 1.5 phút.

- **Mô hình mới (Multi-stage Dockerfile):**  
  Khi chạy `docker build`, Docker daemon khởi tạo một **Container Builder tạm thời (ephemeral container)** để thực thi Stage 1. Theo nguyên lý thiết kế đảm bảo tính tất định và độc lập (Hermetic/Deterministic Build) của Docker:
  1. Mỗi container builder chạy trong một không gian tên (Mount Namespace) hoàn toàn biệt lập, sở hữu một filesystem mới tinh và trống rỗng tại `/root/.gradle` hoặc `/home/gradle/.gradle`.
  2. Môi trường build bên trong container **không có quyền và không tự động kế thừa hay ánh xạ (mount)** thư mục `~/.gradle` của máy chủ host vào bên trong.
  3. Hệ quả là ở mỗi lần pipeline kích hoạt, Gradle bắt buộc phải kết nối ra internet để tải lại toàn bộ thư viện dependencies từ đầu, làm phát sinh thêm 3-4 phút chờ đợi.

### 3. Hiểu Lầm Về Cấu Hình Docker-out-of-Docker (DooD)

Cấu hình chia sẻ socket (`/var/run/docker.sock`) thực chất chỉ là một cơ chế giao tiếp IPC (Unix Domain Socket) cho phép Runner ra lệnh cho Docker daemon trên host tạo và quản lý container. **DooD hoàn toàn không chia sẻ hay tự động mount hệ thống tệp (filesystem/volumes)** giữa máy chủ host và container builder. 

---

**Kết luận:** Container builder của Multi-stage bị cách ly hoàn toàn với filesystem của host. Đây là bản chất cô lập cốt lõi của Docker Container nhằm bảo đảm tính độc lập môi trường, chứ không phải lỗi hạ tầng hay mạng.
