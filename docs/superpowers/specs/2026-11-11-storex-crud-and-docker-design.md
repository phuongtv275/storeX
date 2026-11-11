# Storex Backend CRUD and Docker Environment Design

## Overview
This specification defines the architecture, data models, API endpoints, Docker containerization, and Docker Compose orchestration for the `storex` Spring Boot application.

## Goals
- Implement a clean MVC-layered CRUD service for `Product`.
- Configure Spring Data JPA with PostgreSQL.
- Create a `Dockerfile` to build an image named `storex-backend`.
- Configure `docker-compose.yml` to run `storex-backend` alongside PostgreSQL without a `build:` block in docker-compose.
- Verify end-to-end functionality via automated/manual HTTP tests.

## Architecture & Package Structure
The application follows standard Spring Boot MVC / Layered architecture:
```
com.example.storex
├── controller
│   └── ProductController.java
├── exception
│   ├── ErrorResponse.java
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
├── model
│   ├── dto
│   │   ├── ProductRequestDto.java
│   │   └── ProductResponseDto.java
│   └── entity
│       └── Product.java
├── repository
│   └── ProductRepository.java
└── service
    ├── ProductService.java
    └── impl
        └── ProductServiceImpl.java
```

## Data Model & DTOs
### Entity: `Product`
- Table: `products`
- Fields:
  - `id`: `Long`, `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`
  - `name`: `String`, `@Column(nullable = false)`
  - `price`: `BigDecimal`, `@Column(nullable = false)`
  - `quantity`: `Integer`, `@Column(nullable = false)`
  - `description`: `String`, `@Column`
- Annotations: `@Entity`, `@Table(name = "products")`, Lombok `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`.

### DTOs
- `ProductRequestDto`:
  - `name`: `String`
  - `price`: `BigDecimal`
  - `quantity`: `Integer`
  - `description`: `String`
- `ProductResponseDto`:
  - `id`: `Long`
  - `name`: `String`
  - `price`: `BigDecimal`
  - `quantity`: `Integer`
  - `description`: `String`

## Persistence & Application Configuration
`src/main/resources/application.yaml`:
```yaml
spring:
  application:
    name: storex
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/storex}
    username: ${SPRING_DATASOURCE_USERNAME:postgres}
    password: ${SPRING_DATASOURCE_PASSWORD:secret}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
server:
  port: 8080
```

## Service Layer
`ProductService`:
- `List<ProductResponseDto> getAllProducts()`
- `ProductResponseDto getProductById(Long id)`
- `ProductResponseDto createProduct(ProductRequestDto requestDto)`
- `ProductResponseDto updateProduct(Long id, ProductRequestDto requestDto)`
- `void deleteProduct(Long id)`

`ProductServiceImpl`:
- Injects `ProductRepository`.
- Converts between `Product` entities and DTOs.
- Throws `ResourceNotFoundException("Product not found with id: " + id)` when not found.

## Web MVC Controller Layer
`ProductController`:
- Base path: `/api/products`
- Endpoints:
  - `GET /api/products`: 200 OK -> `List<ProductResponseDto>`
  - `GET /api/products/{id}`: 200 OK -> `ProductResponseDto`
  - `POST /api/products`: 201 Created -> `ProductResponseDto`
  - `PUT /api/products/{id}`: 200 OK -> `ProductResponseDto`
  - `DELETE /api/products/{id}`: 204 No Content

## Exception Handling
- `ResourceNotFoundException`: extends `RuntimeException`.
- `ErrorResponse`:
  - `timestamp`: `LocalDateTime`
  - `status`: `int`
  - `error`: `String`
  - `message`: `String`
- `GlobalExceptionHandler`:
  - `@ExceptionHandler(ResourceNotFoundException.class)` -> returns 404 NOT FOUND with `ErrorResponse`.
  - `@ExceptionHandler(Exception.class)` -> returns 500 INTERNAL_SERVER_ERROR with `ErrorResponse`.

## Docker Containerization & Compose Orchestration
### Dockerfile
- Placed in project root `/Dockerfile`.
- Uses base image `eclipse-temurin:21-jre-alpine`.
- Copies `build/libs/*.jar` to `app.jar`.
- Exposes port `8080`.
- Entrypoint: `["java", "-jar", "app.jar"]`.

### Docker Compose (`docker-compose.yml`)
- Does not contain build instructions (as requested, backend image `storex-backend` is pre-built).
- Services:
  - `backend`:
    - `image: storex-backend:latest`
    - `ports: ["8080:8080"]`
    - `environment`:
      - `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/storex`
      - `SPRING_DATASOURCE_USERNAME=postgres`
      - `SPRING_DATASOURCE_PASSWORD=secret`
    - `depends_on`:
      `postgres`: `condition: service_healthy`
  - `postgres`:
    - `image: postgres:13`
    - `ports: ["5432:5432"]`
    - `environment`:
      - `POSTGRES_DB=storex`
      - `POSTGRES_USER=postgres`
      - `POSTGRES_PASSWORD=secret`
    - `healthcheck`:
      - `test: ["CMD-SHELL", "pg_isready -U postgres -d storex"]`
      - `interval: 3s`
      - `timeout: 5s`
      - `retries: 5`

## Verification Plan
1. Local build: `./gradlew clean bootJar`
2. Build Docker image: `docker build -t storex-backend:latest .`
3. Run containers: `docker compose up -d`
4. End-to-end API testing using curl:
   - `POST /api/products` (create laptop) -> verify 201 Created.
   - `GET /api/products` -> verify list containing created product.
   - `GET /api/products/1` -> verify retrieval by id.
   - `PUT /api/products/1` -> verify update.
   - `DELETE /api/products/1` -> verify 204 No Content.
   - `GET /api/products/1` -> verify 404 Not Found.
5. Check container logs (`docker compose logs backend`) to confirm no Hibernate / connection errors.
