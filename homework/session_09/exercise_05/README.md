# BÁO CÁO PHÂN TÍCH & TỐI ƯU HÓA DOCKER IMAGE BẰNG MULTI-STAGE BUILD
## Dịch Vụ: Notification Service (Hệ Thống Microservices)

---

## 1. TỔNG QUAN TÌNH HUỐNG NGHIỆP VỤ

Trong quá trình triển khai hệ thống Microservices, đội ngũ DevOps phát hiện Docker image của **Notification Service** có kích thước quá lớn, lên tới **~850MB**, mặc dù dung lượng mã nguồn và logic nghiệp vụ thực tế chỉ vỏn vẹn vài chục KB.

### Nguyên Nhân Gốc Rễ Của Vấn Đề (Root Cause)
Dockerfile ban đầu được xây dựng theo mô hình **Single-stage build** với các hạn chế nghiêm trọng:
1. **Bao gồm toàn bộ bộ công cụ biên dịch (JDK & Gradle):** Image sử dụng base image nặng chứa đầy đủ bộ JDK (chứa `javac`, `javadoc`, `jdb`,...) và bộ cài đặt Gradle. Các công cụ này chỉ có tác dụng trong quá trình biên dịch code, hoàn toàn vô dụng và dư thừa khi ứng dụng chạy ở môi trường Production.
2. **Tồn đọng toàn bộ mã nguồn (`src/`) và Gradle Cache:** Quá trình chạy `./gradlew build` bên trong container sinh ra thư mục cache dependencies (`.gradle/caches`) có dung lượng lên đến hàng trăm MB.
3. **Sử dụng trực tiếp image cồng kềnh đó để chạy ứng dụng ở Runtime:** Mọi layer sinh ra trong quá trình build đều bị giữ lại trong image cuối cùng phân phối tới môi trường Production.

---

## 2. BẢNG SO SÁNH: SINGLE-STAGE BUILD VS. MULTI-STAGE BUILD

Dưới đây là bảng phân tích chi tiết so sánh giữa hai kiến trúc đóng gói:

| Tiêu Chí So Sánh | Single-Stage Build (Cũ) | Multi-Stage Build (Đề Xuất Mới) | Đánh Giá Ưu Thế |
|:---|:---|:---|:---:|
| **Cơ chế hoạt động** | Tất cả các bước (Copy code, cài đặt Gradle, biên dịch, chạy ứng dụng) diễn ra trong cùng một môi trường / một `FROM`. | Tách biệt thành 2 giai đoạn riêng biệt:<br>- **Stage 1 (Builder):** Môi trường biên dịch.<br>- **Stage 2 (Runtime):** Chỉ giữ môi trường thực thi tối thiểu. | **Multi-stage** vượt trội về sự tách biệt trách nhiệm (Separation of Concerns). |
| **Dung lượng Image dự kiến** | **~850MB – 1GB** | **~150MB – 200MB** (Giảm tới **75% – 82%**) | **Multi-stage** tối ưu vượt bậc. |
| **Thành phần trong Image cuối** | Chứa OS base (Ubuntu/Debian), JDK đầy đủ, Gradle tools, mã nguồn (`src/`), file cấu hình dev, Gradle cache, file `.jar`. | Chỉ chứa OS Alpine siêu nhẹ, JRE (Java Runtime Environment) và **duy nhất** file `.jar` thành phẩm. | **Multi-stage** loại bỏ 100% rác thừa sau build. |
| **Bảo mật mã nguồn (Source Code Security)** | **KÉM:** Mã nguồn gốc (`.java`, `build.gradle`, tài nguyên nội bộ) nằm trực tiếp bên trong container. Bất kỳ ai truy cập container (`docker exec`) đều đọc được toàn bộ code. | **RẤT CAO:** Thư mục `src/` và toàn bộ công cụ phát triển bị hủy bỏ sau Stage 1. Image xuất xưởng chỉ có bytecode nhị phân (`.jar`), bảo vệ tối đa tài sản trí tuệ. | **Multi-stage** bảo vệ mã nguồn tuyệt đối. |
| **Bề mặt tấn công (Attack Surface & CVEs)** | **LỚN:** Chứa hàng nghìn package, tiện ích dòng lệnh, compiler và thư viện hỗ trợ build, làm tăng nguy cơ dính các lỗ hổng bảo mật (CVEs). | **CỰC KỲ NHỎ:** Nền tảng Alpine Linux tối giản cùng JRE thu hẹp tối đa các công cụ shell và binary, giảm thiểu tối đa các lỗ hổng hệ thống. | **Multi-stage** đạt chuẩn bảo mật Production. |
| **Tốc độ Push / Pull & Triển khai CI/CD** | **CHẬM:** Tốn thời gian đẩy lên Docker Registry và kéo về server triển khai; dễ gây nghẽn băng thông mạng nội bộ/cloud. | **NHANH GẤP 4 - 5 LẦN:** Dung lượng nhỏ giúp việc deploy, scale ngang (auto-scaling) và khởi động container diễn ra gần như tức thì. | **Multi-stage** tăng tốc chu kỳ phát hành sản phẩm. |
| **Chi phí lưu trữ (Storage & Egress Cost)** | **CAO:** Chi phí lưu trữ image trên Registry (ECR, GCR, DockerHub) và chi phí truyền tải dữ liệu qua Internet tăng cao theo số lượng version. | **TIẾT KIỆM ĐÁNG KỂ:** Tiết kiệm ~700MB trên mỗi lần build và mỗi replica triển khai. | **Multi-stage** tiết kiệm ngân sách vận hành hạ tầng. |
| **Ưu điểm** | Cấu hình đơn giản, dễ viết cho người mới bắt đầu. | Dung lượng siêu nhẹ, bảo mật cao, tốc độ cao, đạt chuẩn Enterprise. | **Multi-stage** là tiêu chuẩn bắt buộc cho Cloud-Native. |
| **Nhược điểm** | Quá nặng, bảo mật kém, triển khai chậm, tốn tài nguyên. | Cú pháp Dockerfile cần hiểu biết về cơ chế multi-stage và copy artifact giữa các stage. | — |

---

## 3. THIẾT KẾ GIẢI PHÁP: DOCKERFILE MULTI-STAGE BUILD

Giải pháp được thiết kế theo đúng yêu cầu nghiệp vụ với 2 stage:
- **Stage 1 (Builder):** Sử dụng `gradle:7.6-jdk17` để tải dependencies và biên dịch code ra file `.jar`.
- **Stage 2 (Runtime):** Sử dụng `eclipse-temurin:17-jre-alpine` để chạy ứng dụng, chỉ copy file `.jar` từ thư mục `/home/gradle/project/build/libs/` của stage `builder`.

### Nội Dung File `Dockerfile` Chuẩn Xác

```dockerfile
# ==============================================================================
# Stage 1: Build Stage (Builder)
# Sử dụng image gradle:7.6-jdk17 chứa đầy đủ JDK 17 và môi trường Gradle để build
# ==============================================================================
FROM gradle:7.6-jdk17 AS builder

# Thiết lập thư mục làm việc cho quá trình build
WORKDIR /home/gradle/project

# Copy toàn bộ mã nguồn vào thư mục làm việc của stage builder
COPY --chown=gradle:gradle . .

# Đổi quyền thực thi cho Gradle wrapper và chạy lệnh build ứng dụng (bỏ qua unit test)
RUN chmod +x gradlew && ./gradlew clean build -x test

# ==============================================================================
# Stage 2: Runtime Stage (Production Image)
# Sử dụng image eclipse-temurin:17-jre-alpine siêu nhẹ chỉ chứa Java Runtime
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine

# Thiết lập thư mục chạy ứng dụng
WORKDIR /app

# Copy DUY NHẤT file .jar thành phẩm từ stage builder sang stage runtime
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

# Khai báo port mặc định cho Notification Service
EXPOSE 8080

# Thiết lập lệnh khởi chạy ứng dụng Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 4. PHÂN TÍCH KỸ THUẬT VỀ CÁC DÒNG LỆNH TRONG DOCKERFILE

1. **`FROM gradle:7.6-jdk17 AS builder`**:
   - Khởi tạo Stage 1 với bí danh `builder`. Image này chuẩn bị sẵn môi trường Gradle 7.6 và JDK 17 chính thức, đảm bảo việc build diễn ra ổn định, nhất quán giữa mọi máy developer và hệ thống CI/CD.
2. **`WORKDIR /home/gradle/project` & `COPY --chown=gradle:gradle . .`**:
   - Khởi tạo thư mục làm việc đúng chuẩn người dùng `gradle` không đặc quyền (non-root) của image chính thức, giúp tránh các lỗi phân quyền file khi Gradle tạo thư mục build.
3. **`RUN chmod +x gradlew && ./gradlew clean build -x test`**:
   - Cấp quyền thực thi cho Gradle Wrapper và tiến hành build tạo file `.jar`. Tùy chọn `-x test` giúp bỏ qua unit test trong khâu đóng gói nhằm tăng tốc độ đóng gói (unit test nên được chạy ở một stage riêng trong CI pipeline).
4. **`FROM eclipse-temurin:17-jre-alpine`**:
   - Bắt đầu Stage 2. Đây là một image hoàn toàn mới và độc lập. Tất cả layer trung gian của Stage 1 (mã nguồn, công cụ build, file tạm) sẽ bị Docker tự động giải phóng, không đưa vào image kết quả.
   - Alpine Linux chỉ nặng ~5MB kết hợp với Temurin JRE 17 headless tạo ra môi trường thực thi hoàn hảo với tổng dung lượng chỉ ~140MB.
5. **`COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar`**:
   - Điểm mấu chốt của Multi-stage: Chỉ trích xuất tệp nhị phân `.jar` đã build xong từ stage `builder` mang sang. Không có bất kỳ file mã nguồn `.java` nào lọt vào image cuối cùng.
6. **`ENTRYPOINT ["java", "-jar", "app.jar"]`**:
   - Chạy ứng dụng dưới dạng tiến trình chính (PID 1) của container, giúp container xử lý tín hiệu tắt (`SIGTERM`, `SIGINT`) một cách chuẩn xác (graceful shutdown).

---

## 5. HƯỚNG DẪN THỰC THI & KIỂM CHỨNG

Để build và kiểm tra dung lượng của image sau khi tối ưu:

```bash
# Di chuyển vào thư mục dịch vụ hoặc vị trí chứa Dockerfile
cd homework/session_09/exercise_05

# Tiến hành build Docker image
docker build -t notification-service:v2 -f Dockerfile .

# Kiểm tra dung lượng image kết quả
docker images notification-service:v2
```

**Kết quả kỳ vọng:**
- Kích thước image thu được sẽ dao động trong khoảng **~160MB – 180MB**, đạt mục tiêu đề ra (150-200MB) và giảm hơn 75% so với mức 850MB ban đầu.
