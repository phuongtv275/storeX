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
