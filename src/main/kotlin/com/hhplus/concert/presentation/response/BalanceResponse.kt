package com.hhplus.concert.presentation.response

object BalanceResponse {
    data class Detail(
        val userId: Long,
        val currentAmount: Int,
    )
}
