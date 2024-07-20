package com.hhplus.concert.interfaces.scheduler

import com.hhplus.concert.business.application.service.QueueService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class QueueScheduler(
    private val queueService: QueueService,
) {
    private val logger = LoggerFactory.getLogger(QueueScheduler::class.java)

    @Scheduled(fixedRate = 60000)
    fun maintainProcessingCount() {
        logger.info("Maintain Processing Scheduler Executed")
        queueService.maintainProcessingCount()
    }

    @Scheduled(fixedRate = 60000)
    fun cancelExpiredWaitingQueue() {
        logger.info("Cancel Expired Waiting Queue Executed")
        queueService.cancelExpiredWaitingQueue()
    }
}
