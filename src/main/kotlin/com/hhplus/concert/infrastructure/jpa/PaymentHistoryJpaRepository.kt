package com.hhplus.concert.infrastructure.jpa

import com.hhplus.concert.infrastructure.entity.PaymentHistory
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentHistoryJpaRepository : JpaRepository<PaymentHistory, Long>
