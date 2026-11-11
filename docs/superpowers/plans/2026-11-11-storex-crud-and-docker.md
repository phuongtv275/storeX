# Storex CRUD & Docker Environment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete MVC CRUD service for `Product` in Spring Boot, containerize it via a `Dockerfile` into image `storex-backend`, and orchestrate it with PostgreSQL using `docker-compose.yml`.

**Architecture:** Standard Spring Boot layered MVC architecture (Controller -> Service -> Repository -> PostgreSQL) with DTO abstraction and global exception handling. Single-stage Alpine JRE Dockerfile consuming pre-built JAR. Docker compose orchestrating pre-built `storex-backend:latest` and healthy `postgres:13`.

**Tech Stack:** Java 21, Spring Boot 4.1.2 (Data JPA, Web MVC), PostgreSQL, Lombok, H2 (for isolated testing), Docker, Docker Compose.

**Spec:** [docs/superpowers/specs/2026-11-11-storex-crud-and-docker-design.md](file:///home/trgphun/projects/personal/rikkei/module5/storex/docs/superpowers/specs/2026-11-11-storex-crud-and-docker-design.md)

## Global Constraints
- Language: Java 21 toolchain with Gradle
- Base Docker Image: `eclipse-temurin:21-jre-alpine`
- Docker image name: `storex-backend:latest`
- Docker Compose backend service must NOT contain a `build:` block; it must run `image: storex-backend:latest`
- PostgreSQL service name in Compose: `postgres`
- Datasource URL in Compose: `jdbc:postgresql://postgres:5432/storex`

## Review Focus
1. **Database connection failure on container startup:** Backend container starting before PostgreSQL is ready. Add pg_isready healthcheck to postgres service and `condition: service_healthy` to backend.
2. **Missing product ID on GET/PUT/DELETE:** Accessing non-existent product ID returning 500 instead of 404. Ensure `ResourceNotFoundException` is caught by `GlobalExceptionHandler` returning `404 Not Found`.
3. **Invalid or null fields in Product creation:** Handled gracefully with clean error message.
4. **Empty product list:** `GET /api/products` returning `200 OK` with an empty JSON array `[]`, not `null` or 500 error.
5. **Port conflict or localhost binding mismatch:** In Docker network, communication between containers must use service hostname `postgres`, while host machine accesses ports `8080` and `5432`.

---

### Task 1: Testing Support, Persistence Configuration & Data Model

**Files:**
- Modify: `build.gradle`
- Create: `src/test/resources/application.yaml`
- Modify: `src/main/resources/application.yaml`
- Create: `src/main/java/com/example/storex/model/entity/Product.java`
- Create: `src/main/java/com/example/storex/model/dto/ProductRequestDto.java`
- Create: `src/main/java/com/example/storex/model/dto/ProductResponseDto.java`
- Create: `src/main/java/com/example/storex/repository/ProductRepository.java`
- Create: `src/test/java/com/example/storex/repository/ProductRepositoryTest.java`

**Interfaces:**
- Consumes: None (base layer)
- Produces:
  - `Product`: entity with fields `Long id`, `String name`, `BigDecimal price`, `Integer quantity`, `String description`
  - `ProductRequestDto`: DTO with `name`, `price`, `quantity`, `description`
  - `ProductResponseDto`: DTO with `id`, `name`, `price`, `quantity`, `description`
  - `ProductRepository`: `JpaRepository<Product, Long>`

- [ ] **Step 1: Add H2 test dependency and configure test application.yaml**
Add `testRuntimeOnly 'com.h2database:h2'` to `build.gradle` and configure `src/test/resources/application.yaml` with H2 in-memory settings so local tests run without external PostgreSQL.

- [ ] **Step 2: Run existing tests to verify context loads**
Run: `./gradlew test`
Expected: BUILD SUCCESSFUL (StorexApplicationTests passes).

- [ ] **Step 3: Write failing repository test**
Create `ProductRepositoryTest.java` verifying saving and finding a `Product` by id.
Run: `./gradlew test --tests ProductRepositoryTest`
Expected: FAIL (Product and ProductRepository not found).

- [ ] **Step 4: Implement `Product`, DTOs, `ProductRepository`, and update main `application.yaml`**
- Implement `Product.java` with `@Entity`, `@Table(name = "products")`, `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`.
- Implement `ProductRequestDto.java` and `ProductResponseDto.java`.
- Implement `ProductRepository.java` extending `JpaRepository<Product, Long>`.
- Update `src/main/resources/application.yaml` with `spring.jpa.hibernate.ddl-auto: update` and show-sql.

- [ ] **Step 5: Run repository test to verify it passes**
Run: `./gradlew test --tests ProductRepositoryTest`
Expected: PASS.

- [ ] **Step 6: Commit Task 1**
```bash
git add build.gradle src/
git commit -m "feat: add Product entity, repository, DTOs, and test infrastructure"
```

---

### Task 2: Service Layer & Global Exception Handling

**Files:**
- Create: `src/main/java/com/example/storex/exception/ResourceNotFoundException.java`
- Create: `src/main/java/com/example/storex/exception/ErrorResponse.java`
- Create: `src/main/java/com/example/storex/exception/GlobalExceptionHandler.java`
- Create: `src/main/java/com/example/storex/service/ProductService.java`
- Create: `src/main/java/com/example/storex/service/impl/ProductServiceImpl.java`
- Create: `src/test/java/com/example/storex/service/ProductServiceTest.java`

**Interfaces:**
- Consumes: `Product`, `ProductRepository`, `ProductRequestDto`, `ProductResponseDto`
- Produces:
  - `ProductService`:
    - `List<ProductResponseDto> getAllProducts()`
    - `ProductResponseDto getProductById(Long id)`
    - `ProductResponseDto createProduct(ProductRequestDto requestDto)`
    - `ProductResponseDto updateProduct(Long id, ProductRequestDto requestDto)`
    - `void deleteProduct(Long id)`
  - `ResourceNotFoundException`: unchecked exception with message
  - `GlobalExceptionHandler`: `@RestControllerAdvice` catching `ResourceNotFoundException` (404) and `Exception` (500)

- [ ] **Step 1: Write failing service unit test**
Create `ProductServiceTest.java` using Mockito mocking `ProductRepository` covering:
- `getAllProducts()` returns list of DTOs
- `getProductById(1L)` returns DTO when found
- `getProductById(99L)` throws `ResourceNotFoundException` when not found
- `createProduct(requestDto)` saves entity and returns DTO
- `updateProduct(1L, requestDto)` updates and returns DTO
- `deleteProduct(1L)` deletes entity when exists, throws exception when not found.
Run: `./gradlew test --tests ProductServiceTest`
Expected: FAIL (Service classes do not exist).

- [ ] **Step 2: Implement Exception classes, `ProductService`, and `ProductServiceImpl`**
- Create `ResourceNotFoundException.java` extending `RuntimeException`.
- Create `ErrorResponse.java` with `LocalDateTime timestamp`, `int status`, `String error`, `String message`.
- Create `GlobalExceptionHandler.java` with `@RestControllerAdvice` and `@ExceptionHandler`.
- Create `ProductService.java` interface.
- Create `ProductServiceImpl.java` implementing `ProductService`, injecting `ProductRepository`.

- [ ] **Step 3: Run service tests to verify they pass**
Run: `./gradlew test --tests ProductServiceTest`
Expected: PASS.

- [ ] **Step 4: Commit Task 2**
```bash
git add src/
git commit -m "feat: implement ProductService and GlobalExceptionHandler"
```

---

### Task 3: Web MVC Controller Layer

**Files:**
- Create: `src/main/java/com/example/storex/controller/ProductController.java`
- Create: `src/test/java/com/example/storex/controller/ProductControllerTest.java`

**Interfaces:**
- Consumes: `ProductService`, `ProductRequestDto`, `ProductResponseDto`, `ResourceNotFoundException`, `GlobalExceptionHandler`
- Produces:
  - `ProductController` endpoints:
    - `GET /api/products` (200 OK)
    - `GET /api/products/{id}` (200 OK)
    - `POST /api/products` (201 Created)
    - `PUT /api/products/{id}` (200 OK)
    - `DELETE /api/products/{id}` (204 No Content)

- [ ] **Step 1: Write failing controller WebMvc test**
Create `ProductControllerTest.java` with `@WebMvcTest(ProductController.class)` using `MockMvc` and `@MockitoBean ProductService` to test:
- `GET /api/products` -> 200 OK with JSON array
- `GET /api/products/1` -> 200 OK with product JSON
- `GET /api/products/999` -> 404 Not Found
- `POST /api/products` -> 201 Created with created product JSON
- `PUT /api/products/1` -> 200 OK with updated product JSON
- `DELETE /api/products/1` -> 204 No Content
Run: `./gradlew test --tests ProductControllerTest`
Expected: FAIL (ProductController does not exist).

- [ ] **Step 2: Implement `ProductController`**
Create `ProductController.java` with `@RestController`, `@RequestMapping("/api/products")`, `@RequiredArgsConstructor`, and map all 5 endpoints.

- [ ] **Step 3: Run all project tests**
Run: `./gradlew test`
Expected: All tests pass (contextLoads, repository, service, controller).

- [ ] **Step 4: Commit Task 3**
```bash
git add src/
git commit -m "feat: implement ProductController and WebMvc tests"
```

---

### Task 4: Dockerfile, Docker Compose Orchestration & End-to-End Verification

**Files:**
- Create: `Dockerfile`
- Modify: `docker-compose.yml`

**Interfaces:**
- Consumes: `build/libs/*.jar` generated by `./gradlew clean bootJar`
- Produces:
  - Docker image: `storex-backend:latest`
  - Running Docker Compose stack with backend on port 8080 and postgres on port 5432

- [ ] **Step 1: Create `Dockerfile`**
Create `Dockerfile` at repository root using `eclipse-temurin:21-jre-alpine`:
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: Update `docker-compose.yml`**
Update `docker-compose.yml` to:
- Set backend image to `storex-backend:latest` (without `build:` block)
- Point `SPRING_DATASOURCE_URL` to `jdbc:postgresql://postgres:5432/storex`
- Add healthcheck to postgres service
- Configure backend `depends_on: postgres` with `condition: service_healthy`

- [ ] **Step 3: Build application JAR and Docker image**
Run:
```bash
./gradlew clean bootJar
docker build -t storex-backend:latest .
```
Expected: `storex-backend:latest` built successfully.

- [ ] **Step 4: Launch Docker Compose and verify container health**
Run:
```bash
docker compose up -d
docker compose ps
```
Expected: Both `storex-backend` and `postgres` are running and healthy.

- [ ] **Step 5: Execute end-to-end CRUD verification with curl**
Execute:
1. `POST /api/products` with JSON `{"name":"MacBook Pro","price":1999.99,"quantity":10,"description":"Apple M3 Pro Laptop"}` -> 201 Created
2. `GET /api/products` -> 200 OK with list containing MacBook Pro
3. `GET /api/products/1` -> 200 OK with id 1
4. `PUT /api/products/1` with JSON `{"name":"MacBook Pro M3 Max","price":2499.99,"quantity":8,"description":"Upgraded laptop"}` -> 200 OK
5. `DELETE /api/products/1` -> 204 No Content
6. `GET /api/products/1` -> 404 Not Found with ErrorResponse JSON
7. `docker compose logs backend` -> verify clean application logs without exceptions.

- [ ] **Step 6: Commit Task 4**
```bash
git add Dockerfile docker-compose.yml
git commit -m "feat: add Dockerfile, docker-compose configuration, and verify e2e"
```
