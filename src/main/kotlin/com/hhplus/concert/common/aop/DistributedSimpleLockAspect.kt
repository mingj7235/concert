package com.hhplus.concert.common.aop

import com.hhplus.concert.common.annotation.DistributedSimpleLock
import com.hhplus.concert.common.error.code.ErrorCode
import com.hhplus.concert.common.error.exception.BusinessException
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component
import java.util.UUID

@Aspect
@Component
class DistributedSimpleLockAspect(
    private val redisSimpleLock: RedisSimpleLock,
) {
    @Around("@annotation(com.hhplus.concert.common.annotation.DistributedSimpleLock)")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val distributedLock = method.getAnnotation(DistributedSimpleLock::class.java)

        val lockKey = distributedLock.key
        val lockValue = UUID.randomUUID().toString()

        try {
            val acquired =
                redisSimpleLock.tryLock(
                    lockKey,
                    lockValue,
                    distributedLock.leaseTime,
                    distributedLock.timeUnit,
                )
            if (!acquired) {
                throw BusinessException.BadRequest(ErrorCode.Common.BAD_REQUEST)
            }
            return joinPoint.proceed()
        } finally {
            redisSimpleLock.releaseLock(lockKey, lockValue)
        }
    }
}
