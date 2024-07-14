package com.hhplus.concert.infra.jpa

import com.hhplus.concert.infra.entity.PaymentHistory
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentHistoryJpaRepository : JpaRepository<PaymentHistory, Long>
