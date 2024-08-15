package com.hhplus.concert.business.domain.manager.payment

import com.hhplus.concert.business.domain.message.MessageAlarmPayload
import com.hhplus.concert.business.domain.message.MessageClient
import com.hhplus.concert.business.domain.repository.PaymentRepository
import com.hhplus.concert.common.type.AlarmLevel
import com.hhplus.concert.common.type.PaymentStatus
import org.springframework.stereotype.Component

/**
 * Message Client 를 통해 Event Message 를 담당하는 컴포넌트.
 */
@Component
class PaymentMessageSender(
    private val messageClient: MessageClient,
    private val paymentRepository: PaymentRepository,
) {
    /**
     * 외부 API 를 통해 Payment Event 메세지를 전송한다.
     */
    fun sendPaymentEventMessage(paymentId: Long) {
        val payment = paymentRepository.findById(paymentId) ?: return

        messageClient.sendMessage(
            MessageAlarmPayload(
                alarmLevel = getAlarmLevel(payment.paymentStatus),
                subject = getSubject(payment.id, payment.paymentStatus),
                description = getDescription(payment.amount, payment.paymentStatus),
            ),
        )
    }

    private fun getAlarmLevel(paymentStatus: PaymentStatus): AlarmLevel =
        when (paymentStatus) {
            PaymentStatus.COMPLETED -> AlarmLevel.SUCCESS
            PaymentStatus.FAILED -> AlarmLevel.DANGER
        }

    private fun getSubject(
        paymentId: Long,
        paymentStatus: PaymentStatus,
    ): String =
        when (paymentStatus) {
            PaymentStatus.COMPLETED ->
                "Payment Completed 💰 - payment id : $paymentId"
            PaymentStatus.FAILED ->
                "Payment Failed 😇 - payment id : $paymentId"
        }

    private fun getDescription(
        amount: Int,
        paymentStatus: PaymentStatus,
    ): String =
        when (paymentStatus) {
            PaymentStatus.COMPLETED ->
                "Payment amount : $amount"
            PaymentStatus.FAILED ->
                "Payment requested amount: $amount"
        }
}
