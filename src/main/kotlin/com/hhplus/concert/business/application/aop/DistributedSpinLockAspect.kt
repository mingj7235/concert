package com.hhplus.concert.business.application.aop

import com.hhplus.concert.common.annotation.DistributedSpinLock
import com.hhplus.concert.common.error.code.ErrorCode
import com.hhplus.concert.common.error.exception.BusinessException
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Aspect
@Component
class DistributedSpinLockAspect(
    private val redisTemplate: RedisTemplate<String, String>,
) {
    @Around("@annotation(com.hhplus.concert.common.annotation.DistributedSpinLock)")
    fun around(
        joinPoint: ProceedingJoinPoint,
        distributedSpinLock: DistributedSpinLock,
    ): Any? {
        val key = distributedSpinLock.key
        val startTime = System.currentTimeMillis()
        val endTime = startTime + distributedSpinLock.timeUnit.toMillis(distributedSpinLock.waitTime)

        while (System.currentTimeMillis() < endTime) {
            val lockAcquired =
                redisTemplate
                    .opsForValue()
                    .setIfAbsent(key, "locked", distributedSpinLock.leaseTime, distributedSpinLock.timeUnit)

            if (lockAcquired == true) {
                try {
                    return joinPoint.proceed()
                } finally {
                    redisTemplate.delete(key)
                }
            }

            Thread.sleep(distributedSpinLock.spinTime)
        }

        throw BusinessException.BadRequest(ErrorCode.Common.BAD_REQUEST)
    }
}
