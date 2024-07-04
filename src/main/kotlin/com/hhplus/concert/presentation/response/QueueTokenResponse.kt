package com.hhplus.concert.presentation.response

import com.hhplus.concert.common.type.QueueStatus
import java.time.LocalDateTime

object QueueTokenResponse {
    data class Token(
        val tokenId: Long,
        val createdAt: LocalDateTime,
        val expiredAt: LocalDateTime,
    )

    data class Queue(
        val queueId: Long,
        val joinAt: LocalDateTime,
        val status: QueueStatus,
        val remainingWaitListCount: Int,
    )
}
