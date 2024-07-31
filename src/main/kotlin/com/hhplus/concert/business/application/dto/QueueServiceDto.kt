package com.hhplus.concert.business.application.dto

import com.hhplus.concert.common.type.QueueStatus

class QueueServiceDto {
    data class IssuedToken(
        val token: String,
    )

    data class Queue(
        val status: QueueStatus,
        val remainingWaitListCount: Long,
        val estimatedWaitTime: Long,
    )
}
