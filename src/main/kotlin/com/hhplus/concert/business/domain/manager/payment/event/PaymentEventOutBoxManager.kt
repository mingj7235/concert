package com.hhplus.concert.business.domain.manager.payment.event

import com.hhplus.concert.business.domain.entity.PaymentEventOutBox
import com.hhplus.concert.business.domain.repository.PaymentEventOutBoxRepository
import com.hhplus.concert.common.error.code.ErrorCode
import com.hhplus.concert.common.error.exception.BusinessException
import com.hhplus.concert.common.type.EventStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class PaymentEventOutBoxManager(
    private val paymentEventOutBoxRepository: PaymentEventOutBoxRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun saveEventOutBox(
        domainId: Long,
        eventStatus: EventStatus,
    ): PaymentEventOutBox =
        paymentEventOutBoxRepository.save(
            PaymentEventOutBox(
                paymentId = domainId,
                eventStatus = eventStatus,
            ),
        )

    fun findEventByPaymentId(paymentId: Long): PaymentEventOutBox = paymentEventOutBoxRepository.findByPaymentId(paymentId)

    fun updateEventStatus(
        paymentId: Long,
        eventStatus: EventStatus,
    ) {
        val eventOutbox = paymentEventOutBoxRepository.findByPaymentId(paymentId)
        eventOutbox.updateEventStatus(eventStatus)
    }

    fun retryFailedPaymentEvent(): List<PaymentEventOutBox> =
        paymentEventOutBoxRepository.findAllFailedEvent(LocalDateTime.now().minusMinutes(5))

    @Transactional
    fun deletePublishedPaymentEvent() {
        runCatching {
            paymentEventOutBoxRepository.deleteAllPublishedEvent(LocalDateTime.now().minusDays(7))
        }.onFailure {
            throw BusinessException.BadRequest(ErrorCode.Event.BAD_REQUEST)
        }
    }
}
