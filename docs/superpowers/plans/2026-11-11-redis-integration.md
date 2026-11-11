# Redis Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate Redis into the `storex` backend using Spring Cache abstraction with JSON serialization and 10-minute TTL, and add a healthy Redis container into `docker-compose.yml`.

**Architecture:** Spring Data Redis + Spring Cache abstraction with `@EnableCaching`, a configured `RedisCacheManager` with `GenericJackson2JsonRedisSerializer` for human-readable cached objects, cache eviction on mutations, and a health-checked `redis:7-alpine` container in Docker Compose.

**Tech Stack:** Java 21, Spring Boot 4.1.2, Spring Data Redis, Spring Cache, Jackson, Redis 7 Alpine, Docker Compose.

**Spec:** [docs/superpowers/specs/2026-11-11-redis-integration-design.md](file:///home/trgphun/projects/personal/rikkei/module5/storex/docs/superpowers/specs/2026-11-11-redis-integration-design.md)

## Global Constraints
- Language: Java 21 toolchain with Gradle
- Docker Compose backend service must run `image: storex-backend:latest` without a `build:` block
- Redis service name in Compose: `redis`
- Redis port: `6379`
- Tests must continue to run independently with `spring.cache.type: none` in test application.yaml

## Review Focus
1. **Container startup race condition:** Backend container failing to connect to Redis on boot. Add `redis-cli ping` healthcheck and `condition: service_healthy` on backend.
2. **Stale cache on updates or deletions:** Updating or deleting a product leaving outdated data in `products::all` or `product::<id>`. Ensure `@Caching` evicts both collection and item caches.
3. **Local build test failure:** `./gradlew test` failing because Redis is not running locally. Ensure `src/test/resources/application.yaml` has `spring.cache.type: none`.
4. **Serialization incompatibility:** Non-string/non-JSON binary serialization in Redis. Ensure `GenericJackson2JsonRedisSerializer` is configured for cache values.
5. **Persistence of Redis data:** Redis restarting losing cached items. Add named volume `redis_data:/data`.

---

### Task 1: Dependencies, RedisConfig & Application Properties

**Files:**
- Modify: `build.gradle`
- Create: `src/main/java/com/example/storex/config/RedisConfig.java`
- Modify: `src/main/resources/application.yaml`
- Modify: `src/test/resources/application.yaml`
- Create: `src/test/java/com/example/storex/config/RedisConfigTest.java`

**Interfaces:**
- Consumes: Spring Data Redis & Spring Cache starters
- Produces:
  - `RedisConfig`: `@Configuration @EnableCaching` declaring `RedisCacheManager` and `RedisTemplate<String, Object>` beans.

- [ ] **Step 1: Add Redis dependencies to `build.gradle`**
Add:
```groovy
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
implementation 'org.springframework.boot:spring-boot-starter-cache'
```

- [ ] **Step 2: Update application.yaml and test application.yaml**
- `src/main/resources/application.yaml`: add `spring.cache.type: redis` and `spring.data.redis.host: ${SPRING_DATA_REDIS_HOST:localhost}`, `port: ${SPRING_DATA_REDIS_PORT:6379}`.
- `src/test/resources/application.yaml`: add `spring.cache.type: none`.

- [ ] **Step 3: Write failing RedisConfig test**
Create `RedisConfigTest.java` asserting that `RedisConfig`, `RedisCacheManager`, and `RedisTemplate` beans exist in application context.
Run: `./gradlew test --tests RedisConfigTest`
Expected: FAIL (`RedisConfig` not found).

- [ ] **Step 4: Implement `RedisConfig.java`**
Implement `RedisConfig` with `@Configuration`, `@EnableCaching`, `RedisCacheManager` with 10-minute TTL, and `GenericJackson2JsonRedisSerializer`.

- [ ] **Step 5: Run tests to verify pass**
Run: `./gradlew test --tests RedisConfigTest`
Expected: PASS.

- [ ] **Step 6: Commit Task 1**
```bash
git add build.gradle src/
git commit -m "feat: add Redis dependencies, RedisConfig, and application properties"
```

---

### Task 2: Service Layer Caching & Invalidation Annotations

**Files:**
- Modify: `src/main/java/com/example/storex/service/impl/ProductServiceImpl.java`
- Modify: `src/test/java/com/example/storex/service/ProductServiceTest.java`

**Interfaces:**
- Consumes: `ProductServiceImpl`, Spring Cache annotations (`@Cacheable`, `@CacheEvict`, `@Caching`)
- Produces:
  - Caching on `getAllProducts()` -> `products::all`
  - Caching on `getProductById(id)` -> `product::<id>`
  - Eviction on `createProduct` -> evicts `products::all`
  - Eviction on `updateProduct` -> evicts `products::all` and `product::<id>`
  - Eviction on `deleteProduct` -> evicts `products::all` and `product::<id>`

- [ ] **Step 1: Write tests for caching annotations on ProductServiceImpl**
Add reflection or slice test checking `@Cacheable` and `@CacheEvict` annotations exist on `ProductServiceImpl` methods.
Run: `./gradlew test --tests ProductServiceTest`
Expected: FAIL (annotations not present).

- [ ] **Step 2: Add caching annotations to `ProductServiceImpl.java`**
Add:
- `@Cacheable(value = "products", key = "'all'")` on `getAllProducts`
- `@Cacheable(value = "product", key = "#id")` on `getProductById`
- `@CacheEvict(value = "products", key = "'all'")` on `createProduct`
- `@Caching(evict = { @CacheEvict(value = "products", key = "'all'"), @CacheEvict(value = "product", key = "#id") })` on `updateProduct`
- `@Caching(evict = { @CacheEvict(value = "products", key = "'all'"), @CacheEvict(value = "product", key = "#id") })` on `deleteProduct`

- [ ] **Step 3: Run all project tests**
Run: `./gradlew test`
Expected: PASS (all tests pass).

- [ ] **Step 4: Commit Task 2**
```bash
git add src/
git commit -m "feat: add Spring Cache annotations to ProductServiceImpl"
```

---

### Task 3: Docker Compose Redis Service, Image Build & End-to-End Verification

**Files:**
- Modify: `docker-compose.yml`

**Interfaces:**
- Consumes: `storex-backend:latest`, `redis:7-alpine`
- Produces:
  - Updated `docker-compose.yml` with `redis` service
  - Running and healthy Compose stack with Redis caching active

- [ ] **Step 1: Update `docker-compose.yml`**
Add `redis:7-alpine` service with `redis-cli ping` healthcheck, `redis_data` volume, and configure backend environment (`SPRING_DATA_REDIS_HOST=redis`, `SPRING_DATA_REDIS_PORT=6379`) and `depends_on: redis`.

- [ ] **Step 2: Rebuild application JAR and Docker image**
Run:
```bash
./gradlew clean bootJar
docker build -t storex-backend:latest .
```
Expected: `storex-backend:latest` image rebuilt successfully.

- [ ] **Step 3: Launch Docker Compose stack**
Run:
```bash
docker compose up -d
docker compose ps
```
Expected: All 3 containers (`backend`, `postgres`, `redis`) up and healthy.

- [ ] **Step 4: Verify Redis caching and eviction live**
Execute:
1. `POST /api/products` (create sample product).
2. `GET /api/products` and `GET /api/products/1` to populate cache.
3. Verify keys in Redis: `docker compose exec redis redis-cli KEYS "*"` contains `products::all` and `product::1`.
4. Verify JSON content in Redis: `docker compose exec redis redis-cli GET "product::1"` returns JSON string.
5. Update product via `PUT /api/products/1` -> verify `product::1` and `products::all` are evicted.
6. Delete product via `DELETE /api/products/1` -> verify cache stays clear.

- [ ] **Step 5: Commit Task 3**
```bash
git add docker-compose.yml
git commit -m "feat: configure Redis in docker-compose and verify end-to-end caching"
```
