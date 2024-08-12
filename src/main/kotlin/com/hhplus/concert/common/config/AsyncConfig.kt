package com.hhplus.concert.common.config

import org.slf4j.LoggerFactory
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurer {
    override fun getAsyncExecutor(): Executor? =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 5
            maxPoolSize = 10
            queueCapacity = 25
            setThreadNamePrefix(THREAD_NAME_PREFIX)
            initialize()
        }

    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler? =
        AsyncUncaughtExceptionHandler { throwable, method, params ->
            logger.error("Async method '${method.name}' threw an exception", throwable)
            logger.error("Method parameters: ${params.joinToString()}")
        }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
        const val THREAD_NAME_PREFIX = "ConcertAsync"
    }
}
