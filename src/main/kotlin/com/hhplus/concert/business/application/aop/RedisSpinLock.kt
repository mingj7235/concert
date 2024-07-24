package com.hhplus.concert.business.application.aop

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.TimeUnit

@Component
class RedisSpinLock(
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val lockValue = UUID.randomUUID().toString()

    fun tryLock(
        key: String,
        leaseTime: Long,
        timeUnit: TimeUnit,
        waitTime: Long,
        spinTime: Long,
    ): Boolean {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeUnit.toMillis(waitTime)

        while (System.currentTimeMillis() < endTime) {
            val lockAcquired =
                redisTemplate
                    .opsForValue()
                    .setIfAbsent(key, lockValue, leaseTime, timeUnit)

            if (lockAcquired == true) {
                return true
            }

            Thread.sleep(spinTime)
        }

        return false
    }

    fun releaseLock(key: String): Boolean {
        val currentValue = redisTemplate.opsForValue().get(key)
        if (currentValue == lockValue) {
            return redisTemplate.delete(key)
        }
        return false
    }
}
