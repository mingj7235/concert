package com.hhplus.concert.application.dto

object BalanceServiceDto {
    data class Detail(
        val userId: Long,
        val currentAmount: Long,
    )
}
