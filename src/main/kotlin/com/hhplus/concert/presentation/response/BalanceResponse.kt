package com.hhplus.concert.presentation.response

import com.hhplus.concert.application.dto.BalanceServiceDto

object BalanceResponse {
    data class Detail(
        val userId: Long,
        val currentAmount: Long,
    ) {
        companion object {
            fun from(detailDto: BalanceServiceDto.Detail): Detail =
                Detail(
                    userId = detailDto.userId,
                    currentAmount = detailDto.currentAmount,
                )
        }
    }
}
