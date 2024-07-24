package com.hhplus.concert.common.annotation

import java.util.concurrent.TimeUnit

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DistributedSpinLock(
    val key: String,
    val waitTime: Long = 30, // 전체 대기 시간
    val spinTime: Long = 100, // 각 시도 사이의 대기 시간 (밀리초)
    val leaseTime: Long = 10,
    val timeUnit: TimeUnit = TimeUnit.SECONDS,
)
