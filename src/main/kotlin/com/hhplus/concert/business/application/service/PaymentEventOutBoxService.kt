package com.hhplus.concert.business.application.service

import com.hhplus.concert.business.domain.entity.PaymentEventOutBox
import com.hhplus.concert.business.domain.manager.payment.event.PaymentEvent
import com.hhplus.concert.business.domain.manager.payment.event.PaymentEventOutBoxManager
import com.hhplus.concert.business.domain.manager.payment.event.PaymentEventPublisher
import com.hhplus.concert.common.type.EventStatus
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class PaymentEventOutBoxService(
    private val paymentEventOutBoxManager: PaymentEventOutBoxManager,
    @Qualifier("kafka") private val paymentEventPublisher: PaymentEventPublisher,
) {
    /**
     * outbox 를 저장한다.
     */
    fun saveEventOutBox(
        domainId: Long,
        eventStatus: EventStatus,
    ): PaymentEventOutBox =
        paymentEventOutBoxManager.saveEventOutBox(
            domainId = domainId,
            eventStatus = eventStatus,
        )

    /**
     * outbox 의 상태를 변경한다.
     */
    fun updateEventStatus(
        paymentId: Long,
        eventStatus: EventStatus,
    ) {
        paymentEventOutBoxManager.updateEventStatus(
            paymentId = paymentId,
            eventStatus = eventStatus,
        )
    }

    /**
     * kafka 이벤트를 발행한다.
     */
    fun publishPaymentEvent(event: PaymentEvent) {
        paymentEventPublisher.publishPaymentEvent(event)
    }

    /**
     * 실패로 간주하는 이벤트를 재시도한다.
     * 실패 간주 조건 : 5분이 지났음에도 여전히 상태가 INIT 인 Event
     */
    fun retryFailedPaymentEvent() {
        paymentEventOutBoxManager.retryFailedPaymentEvent().forEach {
            paymentEventPublisher.publishPaymentEvent(PaymentEvent(it.paymentId))
        }
    }

    /**
     * 발행된 후 일주일이 지난 EventOutBox 를 삭제한다.
     */
    fun deletePublishedPaymentEvent() {
        paymentEventOutBoxManager.deletePublishedPaymentEvent()
    }
}
