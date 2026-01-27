package com.portal.universe.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 연결 및 Template 설정을 구성하는 클래스입니다.
 * Refresh Token 저장 및 Token Blacklist 관리에 사용됩니다.
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate을 설정합니다.
     * Key와 Value 모두 String 형태로 직렬화하여 저장합니다.
     *
     * @param connectionFactory Redis 연결 팩토리
     * @return RedisTemplate<String, Object>
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key와 Value 직렬화 방식을 String으로 설정
        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);

        return template;
    }
}
