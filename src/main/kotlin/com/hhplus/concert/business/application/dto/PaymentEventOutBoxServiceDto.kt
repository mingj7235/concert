package com.hhplus.concert.business.application.dto

import com.hhplus.concert.common.type.EventStatus

class PaymentEventOutBoxServiceDto {
    data class EventOutBox(
        val id: Long,
        val paymentId: Long,
        val eventStatus: EventStatus,
    )
}
