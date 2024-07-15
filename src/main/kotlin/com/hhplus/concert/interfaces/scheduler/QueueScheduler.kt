package com.hhplus.concert.interfaces.scheduler

import com.hhplus.concert.business.application.service.QueueService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class QueueScheduler(
    private val queueService: QueueService,
) {
    @Scheduled(fixedRate = 60000)
    fun maintainProcessingCount() {
        queueService.maintainProcessingCount()
    }
}
