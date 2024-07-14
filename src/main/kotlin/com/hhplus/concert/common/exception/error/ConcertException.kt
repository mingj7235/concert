package com.hhplus.concert.common.exception.error

open class ConcertException(
    errorMessage: String,
) : RuntimeException(errorMessage) {
    class NotFound : ConcertException("Concert not found")

    class UnAvailable : ConcertException("Concert is unavailable")

    class NotFoundSchedule : ConcertException("Concert Schedule not found")
}
