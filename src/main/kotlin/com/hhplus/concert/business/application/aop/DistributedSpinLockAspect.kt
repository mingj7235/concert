package com.hhplus.concert.business.application.aop

import com.hhplus.concert.common.annotation.DistributedSpinLock
import com.hhplus.concert.common.error.code.ErrorCode
import com.hhplus.concert.common.error.exception.BusinessException
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component

@Aspect
@Component
class DistributedSpinLockAspect(
    private val redisSpinLock: RedisSpinLock,
) {
    @Around("@annotation(com.hhplus.concert.common.annotation.DistributedSpinLock)")
    fun around(
        joinPoint: ProceedingJoinPoint,
        distributedSpinLock: DistributedSpinLock,
    ): Any? {
        val key = distributedSpinLock.key

        if (redisSpinLock.tryLock(
                key,
                distributedSpinLock.leaseTime,
                distributedSpinLock.timeUnit,
                distributedSpinLock.waitTime,
                distributedSpinLock.spinTime,
            )
        ) {
            try {
                return joinPoint.proceed()
            } finally {
                val released = redisSpinLock.releaseLock(key)
            }
        }

        throw BusinessException.BadRequest(ErrorCode.Common.BAD_REQUEST)
    }
}
