# BÁO CÁO PHÂN TÍCH SỰ CỐ CI/CD & CẤU HÌNH PIPELINE CHO USER SERVICE

## Thông Tin Sự Cố
- **Dự án:** QuickBite / StoreX Microservices
- **Module gặp sự cố:** `user-service` (Spring Boot + Gradle)
- **Hệ thống CI/CD:** GitLab CI/CD (`.gitlab-ci.yml`)
- **Triệu chứng lỗi:** Pipeline liên tục báo lỗi `command not found` (hoặc lỗi cú pháp YAML) ngay từ bước khởi tạo pipeline / job. Khách hàng đang cần gấp bản build để demo.

---

## 1. Cấu Hình Ban Đầu Bị Lỗi

```yaml
stages:
  build_app

build_job:
  stage: build_app
  script:
    - ./gradlew clean build -x test
```

---

## 2. Phân Tích Nguyên Nhân Gây Lỗi Khi Chạy Trên GitLab Runner Mặc Định

Khi chạy file cấu hình trên với GitLab Runner mặc định (thường sử dụng Docker Executor), hệ thống sẽ gặp các vấn đề nghiêm trọng sau:

### Lỗi 1: Sai cú pháp khai báo mảng trong YAML (`stages`)
- **Mô tả lỗi:** 
  Từ khóa `stages` trong GitLab CI/CD yêu cầu giá trị là một **mảng danh sách (Sequence/Array)** các tên stage.
- **Biểu hiện:**
  Đoạn mã ban đầu viết:
  ```yaml
  stages:
    build_app
  ```
  Trong chuẩn YAML, cú pháp trên coi `build_app` là một giá trị vô hướng (scalar string) gán cho khóa `stages`, chứ không phải một phần tử của mảng.
- **Hậu quả:** 
  GitLab CI linter / validator sẽ báo lỗi cú pháp ngay khi khởi tạo pipeline:
  ```text
  stages config should be an array of strings
  ```
  Pipeline bị từ chối thực thi ngay từ khâu phân tích cú pháp (Syntax Validation).

---

### Lỗi 2: Thiếu định nghĩa Docker Image chứa môi trường Java & Gradle (`command not found`)
- **Mô tả lỗi:** 
  Cấu hình không chỉ định từ khóa `image:` ở cấp toàn cục (global) hoặc cấp độ job (`build_job`).
- **Cơ chế hoạt động của GitLab Runner:**
  - Khi không khai báo `image:`, GitLab Runner (Docker Executor) sẽ sử dụng Docker image mặc định được cấu hình trong file `config.toml` của Runner.
  - Image mặc định thông thường trên GitLab Shared Runners là `ruby:2.7`, `ruby:3.1` hoặc các image Linux tối giản (`alpine`, `debian-slim`, `ubuntu:latest`).
  - Trong các image này, **Java Development Kit (JDK)** và **Gradle** hoàn toàn **không được cài đặt sẵn**.
- **Hậu quả khi chạy lệnh `./gradlew clean build -x test`:**
  - `./gradlew` (Gradle Wrapper) thực chất là một shell script (`#!/bin/sh` hoặc `#!/usr/bin/env bash`).
  - Khi script này khởi chạy, nó sẽ tìm kiếm binary `java` (thông qua biến `$JAVA_HOME` hoặc tìm trong `$PATH`).
  - Do container mặc định không có Java, Gradle Wrapper sẽ ném ra lỗi:
    ```text
    ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
    Please set the JAVA_HOME variable in your environment to match the
    location of your Java installation.
    ```
    Hoặc trong một số image tối giản không có sẵn Bash/Shell phù hợp, hệ thống sẽ báo trực tiếp:
    ```text
    /bin/sh: ./gradlew: command not found
    ```

---

### Lỗi 3 (Điểm thiếu sót thực tế): Nguy cơ mất quyền thực thi (`Permission Denied`) của script `gradlew`
- **Mô tả:** 
  Khi developer đẩy mã nguồn từ môi trường Windows lên Git hoặc thiết lập file permission không đúng, file `./gradlew` có thể bị mất cờ quyền thực thi (`chmod +x`).
- **Hậu quả:** 
  Khi Runner cố gắng thực thi `./gradlew`, Linux sẽ báo lỗi:
  ```text
  /bin/sh: eval: line X: ./gradlew: Permission denied
  ```
- **Khắc phục:** 
  Cần thêm lệnh `chmod +x ./gradlew` vào mục `before_script` để đảm bảo file luôn có quyền thực thi độc lập với thuộc tính file trên Git repository.

---

### Lỗi 4 (Điểm thiếu sót về nghiệp vụ): Thiếu `artifacts` để lấy file build demo
- **Mô tả:** 
  Mục tiêu nghiệp vụ là **"khách hàng đang cần gấp bản build mới nhất để demo"**.
- **Hậu quả:** 
  Nếu job build thành công nhưng không có cấu hình `artifacts:`, file đóng gói (`.jar`) được sinh ra trong thư mục `build/libs/` sẽ bị xóa ngay khi container của Runner kết thúc (lifecycle của Docker container là ephermal). Không ai có thể tải file `.jar` về để chạy demo.
- **Khắc phục:** 
  Khai báo `artifacts: paths: - build/libs/*.jar` để lưu trữ và cho phép tải trực tiếp file `.jar` từ giao diện web của GitLab.

---

## 3. Nội Dung File `.gitlab-ci.yml` Đã Được Sửa Hoàn Chỉnh

File `.gitlab-ci.yml` chuẩn mực, sửa triệt để các lỗi trên và tối ưu hóa tốc độ build:

```yaml
# ========================================================
# GitLab CI/CD Pipeline - User Service
# Build Spring Boot Application with Gradle & JDK 17
# ========================================================

# 1. Định nghĩa Docker image chứa sẵn môi trường Java (JDK 17) và Gradle
image: gradle:8.5-jdk17

# 2. Khai báo danh sách các stages (sửa đúng cú pháp mảng YAML có dấu '-')
stages:
  - build_app

# 3. Thiết lập biến môi trường để cache Gradle vào thư mục dự án của GitLab Runner
variables:
  GRADLE_USER_HOME: "$CI_PROJECT_DIR/.gradle"

# 4. Cấu hình Cache để tái sử dụng thư viện giữa các lần chạy, tăng tốc độ build
cache:
  key: "$CI_COMMIT_REF_SLUG"
  paths:
    - .gradle/wrapper/
    - .gradle/caches/

# 5. Định nghĩa job build_job thuộc stage build_app
build_job:
  stage: build_app
  before_script:
    # Cấp quyền thực thi cho Gradle Wrapper script đề phòng mất quyền file trên Git
    - chmod +x ./gradlew
  script:
    # Thực hiện clean và build ứng dụng, bỏ qua unit test để build nhanh demo
    - ./gradlew clean build -x test
  artifacts:
    # Lưu lại file artifact (.jar) sau khi build để demo hoặc deploy
    name: "user-service-$CI_COMMIT_SHORT_SHA"
    paths:
      - build/libs/*.jar
    expire_in: 1 week
```

---

## 4. Bảng Tóm Tắt Các Điểm Sửa Đổi & Lý Do

| STT | Vị Trí / Thuộc Tính | Trước Khi Sửa | Sau Khi Sửa | Giải Thích & Lợi Ích |
|:---:|:---|:---|:---|:---|
| **1** | `stages` | `stages:`<br>`  build_app` | `stages:`<br>`  - build_app` | Sửa đúng cú pháp mảng trong YAML (thêm dấu `-`), vượt qua bước validate cấu hình của GitLab CI. |
| **2** | `image` | *(Không khai báo)* | `image: gradle:8.5-jdk17` | Cung cấp môi trường đầy đủ Java 17 (JDK) và Gradle, giải quyết triệt để lỗi `command not found` (thiếu `java`/`gradle`). |
| **3** | `before_script` | *(Không có)* | `chmod +x ./gradlew` | Đảm bảo file script `gradlew` có quyền thực thi trên mọi hệ thống Runner. |
| **4** | `variables` & `cache` | *(Không có)* | Cache `.gradle/` | Lưu lại các dependencies đã tải về, các lần build tiếp theo nhanh hơn gấp nhiều lần. |
| **5** | `artifacts` | *(Không có)* | Lưu `build/libs/*.jar` | Lưu trữ file build `.jar` để người dùng/khách hàng có thể tải về ngay lập tức để chạy demo. |
