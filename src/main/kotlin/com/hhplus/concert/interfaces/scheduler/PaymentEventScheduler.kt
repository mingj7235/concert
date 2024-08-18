package com.hhplus.concert.interfaces.scheduler

import com.hhplus.concert.business.application.service.PaymentEventOutBoxService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PaymentEventScheduler(
    private val paymentEventOutBoxService: PaymentEventOutBoxService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * 발행이 실패 이벤트를 다시 재시도한다.
     * 조건 >
     * - Init 상태인 이벤트
     * - publishedAt 이 현재 시간 기준으로 5분 이상 넘어간 이벤트
     */
    @Scheduled(fixedRate = 60000)
    fun retryFailedPaymentEvent() {
        logger.info("Retry Failed Payment Event Scheduler Executed")
        paymentEventOutBoxService.retryFailedPaymentEvent()
    }

    /**
     * 발행이 완료된 이벤트를 삭제한다.
     * 조건 >
     * - PUBLISHED 상태인 이벤트
     * - publishedAt 이 현재 시간 기준으로 7일 이상 넘어간 이벤트
     */
    @Scheduled(fixedRate = 60000)
    fun deletePublishedPaymentEvent() {
        logger.info("Delete Publish Payment Event Scheduler Executed")
        paymentEventOutBoxService.deletePublishedPaymentEvent()
    }
}
