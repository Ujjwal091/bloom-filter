package com.example.bloom_filter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for the Bloom filter application.
 * <p>
 * This class provides a RedisTemplate for serializing and deserializing objects to/from Redis.
 * The Redis connection is automatically configured by Spring Boot based on the properties
 * defined in application.properties.
 */
@Configuration
public class RedisConfig {

    /**
     * Creates a RedisTemplate configured for storing serializable objects with String keys.
     * <p>
     * This template is used for storing and retrieving the Bloom filter from Redis.
     *
     * @param connectionFactory the Redis connection factory (automatically provided by Spring Boot)
     * @return a configured RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JdkSerializationRedisSerializer());
        return template;
    }
}