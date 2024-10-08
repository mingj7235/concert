package com.hhplus.concert.interfaces.presentation.response

import com.hhplus.concert.business.application.dto.BalanceServiceDto

class BalanceResponse {
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
