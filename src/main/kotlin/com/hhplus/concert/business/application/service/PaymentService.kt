package com.hhplus.concert.business.application.service

import com.hhplus.concert.business.application.dto.PaymentServiceDto
import com.hhplus.concert.business.domain.manager.payment.PaymentManager
import com.hhplus.concert.business.domain.manager.queue.QueueManager
import com.hhplus.concert.business.domain.manager.reservation.ReservationManager
import com.hhplus.concert.business.domain.manager.user.UserManager
import com.hhplus.concert.common.error.code.ErrorCode
import com.hhplus.concert.common.error.exception.BusinessException
import com.hhplus.concert.common.type.QueueStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentService(
    private val userManager: UserManager,
    private val reservationManager: ReservationManager,
    private val paymentManager: PaymentManager,
    private val queueManager: QueueManager,
) {
    /**
     * 결제를 진행한다.
     * 1. reservation 의 user 와, payment 를 요청하는 user 가 일치하는지 검증
     * 2. payment 수행하고 paymentHistory 에 저장
     * 3. reservation 상태 변경
     * 4. 토큰의 상태 변경 -> completed
     */
    @Transactional
    fun executePayment(
        token: String,
        userId: Long,
        reservationIds: List<Long>,
    ): List<PaymentServiceDto.Result> {
        val user = userManager.findById(userId)
        val requestReservations = reservationManager.findAllById(reservationIds)

        if (requestReservations.isEmpty()) throw BusinessException.BadRequest(ErrorCode.Payment.NOT_FOUND)

        // 결제 요청을 시도하는 user 와 예악한 목록의 user 가 일치하는지 확인한다.
        if (requestReservations.any { it.user.id != userId }) {
            throw BusinessException.BadRequest(ErrorCode.Payment.BAD_REQUEST)
        }

        // 결제를 한다.
        val executedPayments =
            paymentManager.execute(
                user,
                requestReservations,
            )

        // 결제 내역을 저장한다.
        paymentManager.saveHistory(user, executedPayments)

        // reservation 상태를 PAYMENT_COMPLETED 로 변경한다.
        reservationManager.complete(requestReservations)

        // queue 상태를 COMPLETED 로 변경한다.
        val queue = queueManager.findByToken(token)
        queueManager.updateStatus(queue, QueueStatus.COMPLETED)

        // 결과를 반환한다.
        return executedPayments.map {
            PaymentServiceDto.Result(
                paymentId = it.id,
                amount = it.amount,
                paymentStatus = it.paymentStatus,
            )
        }
    }
}
