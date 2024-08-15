package com.hhplus.concert.infrastructure.jpa

import com.hhplus.concert.business.domain.entity.PaymentEventOutBox
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface EventOutBoxJpaRepository : JpaRepository<PaymentEventOutBox, Long> {
    fun findByPaymentId(paymentId: Long): PaymentEventOutBox

    @Query("select peo from PaymentEventOutBox peo where peo.eventStatus = 'INIT' and peo.publishedAt < :dateTime")
    fun findAllFailedEvent(dateTime: LocalDateTime): List<PaymentEventOutBox>

    @Modifying
    @Query("delete from PaymentEventOutBox peo where peo.eventStatus = 'PUBLISHED' and peo.publishedAt < :dateTime")
    fun deleteAllPublishedEvent(dateTime: LocalDateTime)
}
