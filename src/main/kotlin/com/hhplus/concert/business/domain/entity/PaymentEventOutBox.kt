package com.hhplus.concert.business.domain.entity

import com.hhplus.concert.common.type.EventStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDateTime

/**
 * EventOutBox Entity
 */
@Entity
class PaymentEventOutBox(
    paymentId: Long,
    eventStatus: EventStatus,
    publishedAt: LocalDateTime = LocalDateTime.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "payment_id", nullable = false)
    var paymentId: Long = paymentId
        protected set

    @Column(name = "event_status", nullable = false)
    @Enumerated(EnumType.STRING)
    var eventStatus: EventStatus = eventStatus
        protected set

    @Column(name = "published_at", nullable = false)
    var publishedAt: LocalDateTime = publishedAt
        protected set

    fun updateEventStatus(eventStatus: EventStatus) {
        this.eventStatus = eventStatus
    }
}
