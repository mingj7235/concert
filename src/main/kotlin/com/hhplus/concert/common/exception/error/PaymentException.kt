package com.hhplus.concert.common.exception.error

open class PaymentException(
    errorMessage: String,
) : RuntimeException(errorMessage) {
    class InvalidRequest : PaymentException("Invalid payment request")
}
