package com.hhplus.concert.infra.jpa

import com.hhplus.concert.infra.entity.Payment
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentJpaRepository : JpaRepository<Payment, Long> {
}
