package com.hhplus.concert.common.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig(
    val objectMapper: ObjectMapper,
) {
    @Bean
    fun redisCacheManagerBuilderCustomizer(): RedisCacheManagerBuilderCustomizer =
        RedisCacheManagerBuilderCustomizer {
            it
                .withCacheConfiguration(
                    ONE_MIN_CACHE,
                    redisCacheConfigurationByTtl(objectMapper, TTL_ONE_MINUTE),
                ).withCacheConfiguration(
                    FIVE_MIN_CACHE,
                    redisCacheConfigurationByTtl(objectMapper, TTL_FIVE_MINUTE),
                )
        }

    private fun redisCacheConfigurationByTtl(
        objectMapper: ObjectMapper,
        ttlInMin: Long,
    ): RedisCacheConfiguration =
        RedisCacheConfiguration
            .defaultCacheConfig()
            .computePrefixWith { "$it::" }
            .entryTtl(Duration.ofMinutes(ttlInMin))
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    StringRedisSerializer(),
                ),
            ).serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    GenericJackson2JsonRedisSerializer(objectMapper),
                ),
            )

    companion object {
        const val ONE_MIN_CACHE = "one-min-cache"
        const val TTL_ONE_MINUTE = 1L
        const val FIVE_MIN_CACHE = "five-min-cache"
        const val TTL_FIVE_MINUTE = 5L
    }
}
