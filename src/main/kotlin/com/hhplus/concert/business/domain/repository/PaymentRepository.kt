package com.hhplus.concert.business.domain.repository

import com.hhplus.concert.business.domain.entity.Payment

interface PaymentRepository {
    fun save(payment: Payment): Payment

    fun findByReservationId(reservationId: Long): Payment?

    fun findById(paymentId: Long): Payment?
}
