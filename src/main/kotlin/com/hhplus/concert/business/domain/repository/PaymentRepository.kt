package com.hhplus.concert.business.domain.repository

import com.hhplus.concert.infrastructure.entity.Payment

interface PaymentRepository {
    fun save(payment: Payment): Payment

    fun findByReservationId(reservationId: Long): Payment?
}
