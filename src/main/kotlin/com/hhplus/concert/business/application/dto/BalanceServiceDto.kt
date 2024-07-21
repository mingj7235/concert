package com.hhplus.concert.business.application.dto

class BalanceServiceDto {
    data class Detail(
        val userId: Long,
        val currentAmount: Long,
    )
}
