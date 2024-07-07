package com.hhplus.concert.common.exception.error

open class QueueException(
    errorMessage: String,
) : RuntimeException(errorMessage) {
    class QueueNotFound : QueueException("Queue not found")
}
