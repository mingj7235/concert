package com.hhplus.concert.interfaces.presentation.request

object BalanceRequest {
    data class Recharge(
        val amount: Long,
    )
}
