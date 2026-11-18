# BÁO CÁO PHÂN TÍCH SỰ CỐ & ĐỀ XUẤT GIẢI PHÁP CI/CD
## Sự cố: `UnsupportedClassVersionError` trên Notification-Service (Dự án QuickBite)

---

| **Thông tin**       | **Chi tiết**                                                  |
|---------------------|---------------------------------------------------------------|
| **Dự án**           | QuickBite Microservices                                       |
| **Dịch vụ gặp lỗi** | `notification-service`                                        |
| **Môi trường**      | GitHub Actions CI Pipeline (`ci.yml`)                         |
| **Người thực hiện** | DevOps / Tech Lead                                            |
| **Ngày phân tích**  | 18/11/2026                                                    |

---

## 1. TỔNG QUAN SỰ CỐ

Ngay sau khi lập trình viên thực hiện commit và push mã nguồn dịch vụ `notification-service` lên repository, quy trình tích hợp liên tục (CI Pipeline) trên GitHub Actions đã thất bại ngay tại bước build với thông báo lỗi:

```text
java.lang.UnsupportedClassVersionError: class file has been compiled by a more recent version of the Java Runtime (class file version 65.0), this version of the Java Runtime only recognizes class file versions up to 61.0
```

- **Môi trường Local của Developer:** Phát triển và build bình thường bằng JDK 21.
- **Môi trường CI (GitHub Actions):** Gặp lỗi `UnsupportedClassVersionError` khi biên dịch / thực thi ứng dụng.

---

## 2. PHÂN TÍCH NGUYÊN NHÂN GỐC RỄ (ROOT CAUSE ANALYSIS)

### 2.1. Bản chất của lỗi `UnsupportedClassVersionError` trong Pipeline CI

`java.lang.UnsupportedClassVersionError` là một lớp con của `java.lang.ClassFormatError` (thuộc nhóm `LinkageError`). Lỗi này xảy ra khi **Java Virtual Machine (JVM) tại thời điểm thực thi (Runtime)** cố gắng nạp (load) một tệp bytecode (`.class`) được biên dịch bởi một phiên bản trình biên dịch Java (`javac`) **mới hơn** so với phiên bản mà JVM Runtime hiện tại có thể hiểu và hỗ trợ.

Trong kiến trúc của Java, tính tương thích luôn tuân thủ nguyên tắc **Backward Compatibility** (Tương thích ngược):
- Một JVM phiên bản mới (ví dụ Java 21) **có thể chạy** các file `.class` được biên dịch bởi phiên bản cũ hơn (ví dụ Java 8, 11, 17).
- Ngược lại, Java **không hỗ trợ Forward Compatibility** (Tương thích xuôi): Một JVM phiên bản cũ (ví dụ Java 17) **không thể nạp và thực thi** các file `.class` được biên dịch bởi phiên bản mới hơn (ví dụ Java 21).

### 2.2. Vai trò của số hiệu "Class File Version" (65.0, 61.0)

#### Cấu trúc định dạng Bytecode Java
Theo chuẩn đặc tả máy ảo Java (**Java Virtual Machine Specification - JVMS**, Chương 4: *The `class` File Format*), mỗi tệp `.class` đều bắt đầu bằng cấu trúc nhị phân tiêu chuẩn:

```
ClassFile {
    u4             magic;         // 0xCAFEBABE (Magic Number định danh file class)
    u2             minor_version; // Phiên bản phụ (ví dụ: 0)
    u2             major_version; // Phiên bản chính (ví dụ: 61, 65)
    ...
}
```

Khi JVM nạp class, ClassLoader sẽ đọc 2 trường `minor_version` và `major_version`. Nếu `major_version` của file class lớn hơn `major_version` tối đa mà JVM đó hỗ trợ, JVM sẽ lập tức ném ra ngoại lệ `UnsupportedClassVersionError`.

#### Công thức và Bảng ánh xạ Major Version sang Phiên bản Java
Kể từ Java SE 1.2 cho đến các phiên bản hiện đại, quy tắc tính Major Version tuân theo công thức:

$$\text{Major Version} = \text{JDK Version} + 44$$

| Java Platform Version | Major.Minor Version | Trạng thái hỗ trợ | Ghi chú |
|:----------------------|:-------------------:|:------------------|:--------|
| **Java SE 8**         | `52.0`              | LTS (Cũ)          | Phổ biến trong các hệ thống legacy |
| **Java SE 11**        | `55.0`              | LTS               | Phiên bản LTS tiêu chuẩn trước đây |
| **Java SE 17**        | **`61.0`**          | **LTS**           | **Phiên bản JVM đang chạy trên CI** |
| **Java SE 21**        | **`65.0`**          | **LTS**           | **Phiên bản JDK biên dịch code của dev** |
| **Java SE 22**        | `66.0`              | STS               | Short Term Support |
| **Java SE 25**        | `69.0`              | LTS               | Dự kiến LTS kế tiếp |

> **Cơ sở tra cứu chính thức:**
> 1. **Oracle Java Virtual Machine Specification (JVMS SE 21 / SE 17)**: Chương 4, bảng 4.1-A mô tả các hằng số phiên bản class file.
> 2. **Lệnh thực tế để kiểm tra metadata của class:**
>    ```bash
>    javap -verbose MyClass.class | grep "major version"
>    ```
>    Kết quả sẽ hiển thị trực tiếp `major version: 65` (tương ứng JDK 21).

#### Giải mã thông báo lỗi
- `class file version 65.0`: Mã nguồn dịch vụ hoặc thư viện phụ thuộc đã được biên dịch với mục tiêu tương thích bytecode của **Java 21**.
- `only recognizes class file versions up to 61.0`: Máy ảo Java (JVM) được khởi động trong bước build của pipeline CI chỉ là **Java 17**.
- **Kết luận:** JVM Java 17 trên CI bị yêu cầu thực thi hoặc nạp bytecode của Java 21, dẫn đến crash ngay lập tức.

---

### 2.3. Tại sao lỗi không xuất hiện ở Local nhưng lại xảy ra trên GitHub Actions?

Sự đối lập giữa hai môi trường xuất phát từ sự khác biệt cốt lõi về cấu hình và tính chất môi trường:

```
+----------------------------------------------------+
|               MÔI TRƯỜNG LOCAL                     |
|  - Cài đặt: JDK 21 (Adoptium/Temurin, Corretto...) |
|  - JAVA_HOME: Chỉ tới JDK 21                       |
|  - javac: v21  ===>  Output: Bytecode 65.0        |
|  - java (JVM): v21 ===> Nạp tốt Bytecode <= 65.0   |
|  => KẾT QUẢ: BUILD SUCCESS                         |
+----------------------------------------------------+
                          VS
+----------------------------------------------------+
|            GITHUB ACTIONS RUNNER (CI)              |
|  - Image: ubuntu-latest (Môi trường sạch / Ephemeral)|
|  - Workflow ci.yml: setup-java (java-version: 17)   |
|  - JAVA_HOME: Chỉ tới JDK 17                       |
|  - gradle/tests runtime: Chạy trên JVM 17 (v61.0)  |
|  - Đọc class bytecode: 65.0 (từ code / plugins)   |
|  => KẾT QUẢ: BUILD FAILED (UnsupportedClassVersion)|
+----------------------------------------------------+
```

| Đặc điểm | Môi trường Local của Developer | Môi trường GitHub Actions Runner |
|:---|:---|:---|
| **Tính chất môi trường** | Bền vững (*Stateful*), cấu hình tích lũy theo thời gian trên máy cá nhân. | Tạm thời (*Ephemeral / Stateless*), khởi tạo máy ảo mới hoàn toàn cho mỗi lần chạy. |
| **Phiên bản JDK thực thi** | Đã cài đặt JDK 21 làm mặc định trong `PATH` hoặc cấu hình qua IDE (IntelliJ IDEA). | Hoàn toàn phụ thuộc vào khai báo trong file `.github/workflows/ci.yml`. |
| **Nguyên nhân sai lệch (Configuration Drift)** | Developer nâng cấp code hoặc cấu hình `build.gradle` sử dụng Java 21, nhưng **quên cập nhật đồng bộ file workflow CI** (vẫn đang để Java 17). | Pipeline lấy code mới nhất nhưng lại chạy trên runner chỉ thiết lập JDK 17. |

---

## 3. ĐỀ XUẤT GIẢI PHÁP KHẮC PHỤC

### 3.1. Vị trí cụ thể trong file cấu hình CI cần điều chỉnh

Trong các dự án sử dụng GitHub Actions, bước khởi tạo môi trường Java thường sử dụng Action chính thức `actions/setup-java`. 

Vị trí lỗi nằm tại thuộc tính `java-version` trong step **Setup Java**:

#### File workflow trước khi sửa (`.github/workflows/ci.yml` - Bị lỗi):
```yaml
name: Notification Service CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Source Code
        uses: actions/checkout@v4

      # ==============================================================
      # [VỊ TRÍ LỖI]: Cấu hình Java 17 trong khi code yêu cầu Java 21
      # ==============================================================
      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: '17'          # <--- ĐÂY LÀ NGUYÊN NHÂN LỖI (Class version 61.0)
          distribution: 'temurin'
          cache: 'gradle'

      - name: Grant execute permission for gradlew
        run: chmod +x ./gradlew
        working-directory: ./notification-service

      - name: Build with Gradle
        run: ./gradlew bootJar
        working-directory: ./notification-service
```

#### File workflow sau khi chuẩn hóa (`.github/workflows/ci.yml` - Đã khắc phục):
```yaml
name: Notification Service CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Source Code
        uses: actions/checkout@v4

      # ==============================================================
      # [ĐÃ KHẮC PHỤC]: Nâng cấp java-version lên '21' khớp với dự án
      # ==============================================================
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'          # <--- Nâng cấp lên Java 21 (Hỗ trợ Class version 65.0)
          distribution: 'temurin'     # Eclipse Temurin (OpenJDK chuẩn công nghiệp)
          cache: 'gradle'             # Tối ưu thời gian tải dependency

      - name: Grant execute permission for gradlew
        run: chmod +x ./gradlew
        working-directory: ./notification-service

      - name: Build and Test with Gradle
        run: ./gradlew build --info
        working-directory: ./notification-service

      - name: Upload Build Artifact
        uses: actions/upload-artifact@v4
        with:
          name: notification-service-artifact
          path: notification-service/build/libs/*.jar
          retention-days: 7
```

**Giải thích:**
Khi thay đổi `java-version: '21'`, action `actions/setup-java@v4` sẽ tải và cấu hình JDK 21 làm biến môi trường `JAVA_HOME` trên runner. Lúc này JVM thực thi Gradle daemon và chạy các tác vụ build/test là JVM 21, hoàn toàn tương thích với Class file version 65.0.

---

### 3.2. Nguyên tắc chung ngăn chặn sự cố tái diễn (Best Practices)

Để đảm bảo tính toàn vẹn và ngăn chặn triệt để hiện tượng sai lệch môi trường (*Environment Drift*) giữa Local Dev và CI/CD, đội ngũ kỹ thuật cần áp dụng các nguyên tắc chuẩn hóa sau:

```
                               CÁC NGUYÊN TẮC ĐỒNG BỘ MÔI TRƯỜNG
                               
  [ 1. Gradle Java Toolchain ]          [ 2. File Quy Ước Phiên Bản ]
     build.gradle: toolchain               .java-version / .sdkmanrc
               |                                       |
               +-------------------+-------------------+
                                   |
                                   v
             [ 3. Đồng bộ CI đọc tự động (java-version-file) ]
                                   |
                                   v
             [ 4. Containerized Multi-stage Docker Build ]
```

#### 1. Sử dụng Gradle Java Toolchains làm "Single Source of Truth"
Thay vì phụ thuộc vào phiên bản Java cài sẵn trên máy hay runner, hãy khai báo tường minh **Java Toolchain** trong file `build.gradle`:

```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```
*Lợi ích:* Kể cả khi runner CI chạy Gradle bằng Java 17, Gradle sẽ tự động phát hiện, tải về và dùng đúng JDK 21 để compile mã nguồn và thực thi test.

#### 2. Đồng bộ phiên bản qua tệp cấu hình (`.java-version` / `.sdkmanrc`)
Đặt một file `.java-version` tại thư mục gốc của repository:
```text
21
```
Trên máy cá nhân, các công cụ quản lý SDK như **SDKMAN**, **jEnv**, **asdf** sẽ tự động chuyển sang JDK 21 khi developer `cd` vào dự án.
Trên GitHub Actions, cấu hình `actions/setup-java` đọc trực tiếp từ tệp này thay vì hard-code số phiên bản:

```yaml
- name: Set up JDK from file
  uses: actions/setup-java@v4
  with:
    java-version-file: '.java-version'
    distribution: 'temurin'
```
*Lợi ích:* Khi cần nâng cấp Java cho dự án, chỉ cần thay đổi số phiên bản tại duy nhất một file `.java-version`. Cả môi trường local và CI đều tự động cập nhật đồng bộ.

#### 3. Sử dụng Containerized Builds (Docker Multi-stage Build)
Đóng gói quy trình build bên trong một container Docker để đạt tính nhất quán 100%:

```dockerfile
# Giai đoạn Build
FROM gradle:8.6-jdk21 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon

# Giai đoạn Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```
Khi chạy qua Docker, dù chạy trên máy Mac, Windows, Linux hay runner CI, môi trường build đều sử dụng cùng một base image `jdk21`.

#### 4. Thêm bước Fail-Fast kiểm tra phiên bản trên CI
Thêm một bước xác thực môi trường trước khi chạy build:
```yaml
- name: Verify Java Environment
  run: |
    echo "Current Java Version:"
    java -version
    echo "JAVA_HOME is: $JAVA_HOME"
```
Giúp phát hiện ngay phiên bản sai lệch trước khi bước compile tốn thời gian diễn ra.

---

## 4. KẾT LUẬN

| Vấn đề | Đánh giá |
|:---|:---|
| **Hiện tượng** | `UnsupportedClassVersionError: class file 65.0 on runtime 61.0` |
| **Nguyên nhân cốt lõi** | Sai lệch môi trường (Environment Drift): Mã nguồn yêu cầu bytecode Java 21 (`65.0`), trong khi CI runner được cấu hình Java 17 (`61.0`). |
| **Xử lý nhanh** | Sửa `java-version: '21'` trong `.github/workflows/ci.yml`. |
| **Giải pháp dài hạn** | Áp dụng Gradle Toolchain + file cấu hình `.java-version` dùng chung cho cả Local và CI. |
