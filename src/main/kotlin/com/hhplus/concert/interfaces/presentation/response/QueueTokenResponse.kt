package com.hhplus.concert.interfaces.presentation.response

import com.hhplus.concert.business.application.dto.QueueServiceDto
import com.hhplus.concert.common.type.QueueStatus

class QueueTokenResponse {
    data class Token(
        val token: String,
    ) {
        companion object {
            fun from(issuedTokenDto: QueueServiceDto.IssuedToken): Token =
                Token(
                    token = issuedTokenDto.token,
                )
        }
    }

    data class Queue(
        val status: QueueStatus,
        val remainingWaitListCount: Long,
        val estimatedWaitTime: Long,
    ) {
        companion object {
            fun from(queueDto: QueueServiceDto.Queue): Queue =
                Queue(
                    status = queueDto.status,
                    remainingWaitListCount = queueDto.remainingWaitListCount,
                    estimatedWaitTime = queueDto.estimatedWaitTime,
                )
        }
    }
}
