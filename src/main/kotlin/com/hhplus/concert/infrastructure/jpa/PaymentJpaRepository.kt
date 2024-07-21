package com.hhplus.concert.infrastructure.jpa

import com.hhplus.concert.business.domain.entity.Payment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PaymentJpaRepository : JpaRepository<Payment, Long> {
    @Query("select payment from Payment payment where payment.reservation.id = :reservationId")
    fun findByReservationId(reservationId: Long): Payment?
}
