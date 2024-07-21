package com.hhplus.concert.business.application.dto

import com.hhplus.concert.common.type.QueueStatus
import java.time.LocalDateTime

class QueueServiceDto {
    data class IssuedToken(
        val token: String,
        val createdAt: LocalDateTime,
    )

    data class Queue(
        val queueId: Long,
        val joinAt: LocalDateTime,
        val status: QueueStatus,
        val remainingWaitListCount: Int,
    )
}
