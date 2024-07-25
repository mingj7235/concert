package com.hhplus.concert.common.aop

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RedisSimpleLock(
    private val redisTemplate: RedisTemplate<String, String>,
) {
    fun tryLock(
        key: String,
        value: String,
        leaseTime: Long,
        timeUnit: TimeUnit,
    ): Boolean =
        redisTemplate
            .opsForValue()
            .setIfAbsent(key, value, leaseTime, timeUnit) ?: false

    fun releaseLock(
        key: String,
        value: String,
    ): Boolean {
        val ops = redisTemplate.opsForValue()
        val lockValue = ops.get(key)

        if (lockValue == value) {
            redisTemplate.delete(key)
            return true
        }
        return false
    }
}
