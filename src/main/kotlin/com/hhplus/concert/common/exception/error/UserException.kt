package com.hhplus.concert.common.exception.error

open class UserException(
    errorMessage: String,
) : RuntimeException(errorMessage) {
    class UserNotFound : UserException("User not found")
}
