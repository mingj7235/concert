package com.hhplus.concert.common.error.code

import org.springframework.http.HttpStatus

enum class ConcertErrorCode(
    override val httpStatus: HttpStatus,
    override val errorCode: String,
    override val message: String,
) : ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "CONCERT001", "해당 콘서트를 찾을 수 없습니다."),
    UNAVAILABLE(HttpStatus.BAD_REQUEST, "CONCERT002", "해당 콘서트는 이용이 불가능합니다."),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "CONCERT003", "해당 콘서트의 일정을 찾을 수 없습니다."),
}
