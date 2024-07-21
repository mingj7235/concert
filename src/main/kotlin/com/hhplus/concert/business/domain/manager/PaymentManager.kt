package com.hhplus.concert.business.domain.manager

import com.hhplus.concert.business.domain.entity.Payment
import com.hhplus.concert.business.domain.entity.PaymentHistory
import com.hhplus.concert.business.domain.entity.Reservation
import com.hhplus.concert.business.domain.entity.User
import com.hhplus.concert.business.domain.repository.PaymentHistoryRepository
import com.hhplus.concert.business.domain.repository.PaymentRepository
import com.hhplus.concert.common.type.PaymentStatus
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class PaymentManager(
    private val paymentRepository: PaymentRepository,
    private val paymentHistoryRepository: PaymentHistoryRepository,
) {
    /**
     * 결제를 실행한다.
     * - payment 를 저장한다.
     * - 예상치 못한 예외 발생 시, 결제 실패로 저장한다.
     */
    fun execute(
        user: User,
        requestReservations: List<Reservation>,
    ): List<Payment> =
        requestReservations.map { reservation ->
            runCatching {
                Payment(
                    user = user,
                    reservation = reservation,
                    amount = reservation.seat.seatPrice,
                    executedAt = LocalDateTime.now(),
                    paymentStatus = PaymentStatus.COMPLETED,
                ).let { paymentRepository.save(it) }
            }.getOrElse {
                Payment(
                    user = user,
                    reservation = reservation,
                    amount = reservation.seat.seatPrice,
                    executedAt = LocalDateTime.now(),
                    paymentStatus = PaymentStatus.FAILED,
                ).let { paymentRepository.save(it) }
            }
        }

    fun saveHistory(
        user: User,
        payments: List<Payment>,
    ) {
        payments.forEach { payment ->
            paymentHistoryRepository.save(
                PaymentHistory(
                    user = user,
                    payment = payment,
                ),
            )
        }
    }
}