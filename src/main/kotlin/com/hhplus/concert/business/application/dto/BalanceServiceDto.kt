package com.hhplus.concert.business.application.dto

object BalanceServiceDto {
    data class Detail(
        val userId: Long,
        val currentAmount: Long,
    )
}
