package com.hhplus.concert.common.exception.error

open class BalanceException(
    errorMessage: String,
) : RuntimeException(errorMessage) {
    class BalanceNotFound : BalanceException("Balance Not Found")
}
