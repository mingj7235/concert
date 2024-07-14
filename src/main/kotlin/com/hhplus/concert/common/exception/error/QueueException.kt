package com.hhplus.concert.common.exception.error

open class QueueException(
    errorMessage: String,
) : RuntimeException(errorMessage) {
    class QueueNotFound : QueueException("Queue not found")

    class InvalidRequest : QueueException("Invalid request")

    class NotAllowed : QueueException("Queue is not processing")
}
