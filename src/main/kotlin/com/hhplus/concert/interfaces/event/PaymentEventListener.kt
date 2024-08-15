package com.hhplus.concert.interfaces.event

import com.hhplus.concert.business.application.service.PaymentEventOutBoxService
import com.hhplus.concert.business.domain.manager.payment.event.PaymentEvent
import com.hhplus.concert.common.type.EventStatus
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PaymentEventListener(
    private val paymentEventOutBoxService: PaymentEventOutBoxService,
) {
    /**
     * 커밋 전 (Before commit) 에 Outbox 를 저장한다.
     * 1. 커밋 전에 outbox 가 저장 되었으므로, 트랜잭션이 실패했어도 outbox 는 Init 상태로 저장이 된다.
     * 2. 만일 트랜잭션이 실패한 경우의 outbox 는 batch 를 통해 재시도를 하도록 한다.
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun saveEventOutBoxForPaymentCompleted(event: PaymentEvent) {
        logger.info("EventOutBox 저장 - Payment Id : ${event.paymentId}")
        paymentEventOutBoxService.saveEventOutBox(
            domainId = event.paymentId,
            eventStatus = EventStatus.INIT,
        )
    }

    /**
     * 커밋 이후 (After commit) 에는 다음과 같은 일을 수행한다.
     * 1. 해당 outbox 의 상태를 변경한다.
     * 2. kafka event 를 발행한다.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun publishReservationEvent(event: PaymentEvent) {
        paymentEventOutBoxService.publishPaymentEvent(event)
    }

    private val logger = LoggerFactory.getLogger(this::class.java)
}
