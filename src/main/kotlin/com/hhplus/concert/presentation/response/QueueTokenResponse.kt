package com.hhplus.concert.presentation.response

import com.hhplus.concert.application.dto.QueueServiceDto
import com.hhplus.concert.common.type.QueueStatus
import java.time.LocalDateTime

object QueueTokenResponse {
    data class Token(
        val token: String,
        val createdAt: LocalDateTime,
    ) {
        companion object {
            fun from(issuedTokenDto: QueueServiceDto.IssuedToken): Token =
                Token(
                    token = issuedTokenDto.token,
                    createdAt = issuedTokenDto.createdAt,
                )
        }
    }

    data class Queue(
        val queueId: Long,
        val joinAt: LocalDateTime,
        val status: QueueStatus,
        val remainingWaitListCount: Int,
    ) {
        companion object {
            fun from(queueDto: QueueServiceDto.Queue): Queue =
                Queue(
                    queueId = queueDto.queueId,
                    joinAt = queueDto.joinAt,
                    status = queueDto.status,
                    remainingWaitListCount = queueDto.remainingWaitListCount,
                )
        }
    }
}
