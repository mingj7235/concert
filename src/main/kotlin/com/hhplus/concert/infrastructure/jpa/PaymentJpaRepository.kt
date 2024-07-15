package com.hhplus.concert.infrastructure.jpa

import com.hhplus.concert.infrastructure.entity.Payment
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentJpaRepository : JpaRepository<Payment, Long>
