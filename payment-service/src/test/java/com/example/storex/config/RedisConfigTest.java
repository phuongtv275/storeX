package com.example.storex.config;

import com.example.storex.model.dto.PaymentResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    @Test
    @DisplayName("RedisSerializer.json() should serialize and deserialize List of PaymentResponseDto")
    void shouldSerializeAndDeserializeList() {
        RedisSerializer<Object> serializer = RedisSerializer.json();
        List<PaymentResponseDto> list = new ArrayList<>();
        list.add(
                PaymentResponseDto.builder()
                        .id(1L)
                        .orderId(100L)
                        .amount(BigDecimal.valueOf(99.99))
                        .paymentMethod("CREDIT_CARD")
                        .status("PENDING")
                        .build()
        );
        byte[] bytes = serializer.serialize(list);
        Object deserialized = serializer.deserialize(bytes);
        assertThat(deserialized).isInstanceOf(List.class);
        List<?> resultList = (List<?>) deserialized;
        assertThat(resultList).hasSize(1);
        assertThat(resultList.get(0)).isInstanceOf(PaymentResponseDto.class);
    }
}
