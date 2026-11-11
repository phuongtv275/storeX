package com.example.storex.config;

import com.example.storex.model.dto.UserResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

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
    @DisplayName("RedisSerializer.json() should serialize and deserialize ArrayList of UserResponseDto")
    void shouldSerializeAndDeserializeArrayList() {
        RedisSerializer<Object> serializer = RedisSerializer.json();
        ArrayList<UserResponseDto> list = new ArrayList<>();
        list.add(
                UserResponseDto.builder()
                        .id(1L)
                        .username("john_doe")
                        .email("john@example.com")
                        .fullName("John Doe")
                        .build()
        );

        byte[] bytes = serializer.serialize(list);
        assertThat(bytes).isNotNull();

        Object deserialized = serializer.deserialize(bytes);
        assertThat(deserialized).isInstanceOf(List.class);

        List<?> resultList = (List<?>) deserialized;
        assertThat(resultList).hasSize(1);
        assertThat(resultList.get(0)).isInstanceOf(UserResponseDto.class);

        UserResponseDto userDto = (UserResponseDto) resultList.get(0);
        assertThat(userDto.getUsername()).isEqualTo("john_doe");
        assertThat(userDto.getEmail()).isEqualTo("john@example.com");
        assertThat(userDto.getFullName()).isEqualTo("John Doe");
    }
}
