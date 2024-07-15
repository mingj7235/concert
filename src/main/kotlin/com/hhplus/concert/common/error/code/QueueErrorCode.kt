package com.hhplus.concert.common.error.code

import org.springframework.http.HttpStatus

enum class QueueErrorCode(
    override val httpStatus: HttpStatus,
    override val errorCode: String,
    override val message: String,
) : ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "QUEUE001", "해당 대기열을 찾을 수 없습니다."),

    NOT_ALLOWED(HttpStatus.BAD_REQUEST, "QUEUE002", "해당 대기열은 허용되지 않습니다."),
}
