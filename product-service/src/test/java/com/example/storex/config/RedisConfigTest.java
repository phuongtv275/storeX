package com.example.storex.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RedisConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("RedisConfig bean should exist in application context")
    void shouldContainRedisConfigBean() {
        assertThat(applicationContext.containsBean("redisConfig")).isTrue();
    }

    @Test
    @DisplayName("RedisTemplate bean should be configured and present")
    void shouldContainRedisTemplateBean() {
        assertThat(applicationContext.containsBean("redisTemplate")).isTrue();
    }

    @Test
    @DisplayName("When spring.cache.type is none, CacheManager should be NoOpCacheManager")
    void shouldRespectCacheTypeNone() {
        org.springframework.cache.CacheManager cacheManager = applicationContext.getBean(org.springframework.cache.CacheManager.class);
        assertThat(cacheManager).isInstanceOf(org.springframework.cache.support.NoOpCacheManager.class);
    }
}
