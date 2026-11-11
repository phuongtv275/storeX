# Redis Integration Design Specification

## Overview
This specification details the integration of Redis caching into the `storex` Spring Boot application and its containerized environment. It introduces declarative Spring Cache with JSON serialization for Product entities and adds a health-checked Redis service to `docker-compose.yml`.

## Goals
- Add Spring Data Redis and Spring Cache starters to `build.gradle`.
- Provide a `RedisConfig` configuration enabling Spring Cache with a customized `RedisCacheManager`.
- Configure human-readable JSON serialization with a 10-minute TTL.
- Annotate `ProductServiceImpl` with `@Cacheable` and `@Caching`/`@CacheEvict` for collection and item caches.
- Update `application.yaml` with Redis connection parameters and disable caching in test scope (`spring.cache.type: none`).
- Add a `redis:7-alpine` service with healthcheck and persistent volume to `docker-compose.yml`.
- Verify Redis cache persistence, hit/miss, and eviction via `redis-cli` and `curl`.

## Architecture & Configuration
### 1. Dependencies
Add to `build.gradle`:
- `org.springframework.boot:spring-boot-starter-data-redis`
- `org.springframework.boot:spring-boot-starter-cache`

### 2. Configuration Class: `com.example.storex.config.RedisConfig`
- Annotations: `@Configuration`, `@EnableCaching`
- Beans:
  - `RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory)`:
    - Default TTL: 10 minutes (`Duration.ofMinutes(10)`).
    - Null values: disabled (`disableCachingNullValues()`).
    - Key serialization: `new StringRedisSerializer()`.
    - Value serialization: `new GenericJackson2JsonRedisSerializer()` (or Jackson 3 JSON serializer).
  - `RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory)`:
    - Key serializer: `StringRedisSerializer`.
    - Value serializer: `GenericJackson2JsonRedisSerializer`.

### 3. Application Properties
`src/main/resources/application.yaml`:
```yaml
spring:
  cache:
    type: redis
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:localhost}
      port: ${SPRING_DATA_REDIS_PORT:6379}
```

`src/test/resources/application.yaml`:
```yaml
spring:
  cache:
    type: none
```

## Caching Strategy
- **Collection Cache**:
  - Cache name: `products`
  - Key: `'all'`
  - Read: `getAllProducts()` annotated with `@Cacheable(value = "products", key = "'all'")`
  - Invalidation: `createProduct(...)`, `updateProduct(...)`, `deleteProduct(...)` evict `products::all`.

- **Item Cache**:
  - Cache name: `product`
  - Key: `#id`
  - Read: `getProductById(Long id)` annotated with `@Cacheable(value = "product", key = "#id")`
  - Invalidation: `updateProduct(Long id, ...)` and `deleteProduct(Long id)` evict `product::<id>`.

## Docker Compose Configuration
Add `redis` service in `docker-compose.yml`:
```yaml
services:
  backend:
    image: storex-backend:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/storex
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=secret
      - SPRING_DATA_REDIS_HOST=redis
      - SPRING_DATA_REDIS_PORT=6379
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy

  postgres:
    image: postgres:13
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_DB=storex
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=secret
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d storex"]
      interval: 3s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 3s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
  redis_data:
```

## Verification Plan
1. Local test suite: `./gradlew test` (verifies tests pass with `spring.cache.type: none`).
2. Build JAR: `./gradlew clean bootJar`.
3. Build Docker image: `docker build -t storex-backend:latest .`.
4. Run Compose: `docker compose up -d`.
5. Check health: `docker compose ps` (all 3 services running and healthy).
6. Verify caching & eviction:
   - Create product with `POST /api/products`.
   - Query `GET /api/products` and `GET /api/products/1`.
   - Inspect keys with `docker compose exec redis redis-cli KEYS "*"` -> verify keys `products::all` and `product::1`.
   - Inspect value with `docker compose exec redis redis-cli GET "product::1"` -> verify JSON contents.
   - Update product with `PUT /api/products/1` -> verify cache invalidation in Redis.
   - Delete product with `DELETE /api/products/1` -> verify cache invalidation in Redis.
