package com.hhplus.concert.domain.repository

import com.hhplus.concert.infra.entity.Payment

interface PaymentRepository {
    fun save(payment: Payment): Payment
}
