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

    @Column(name = "published_date_time", nullable = false)
    var publishedDateTime: LocalDateTime = LocalDateTime.now()

    fun updateEventStatus(eventStatus: EventStatus) {
        this.eventStatus = eventStatus
    }
}