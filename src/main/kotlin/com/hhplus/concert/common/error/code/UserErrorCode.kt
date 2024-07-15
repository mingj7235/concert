package com.hhplus.concert.common.error.code

import org.springframework.http.HttpStatus

enum class UserErrorCode(
    override val httpStatus: HttpStatus,
    override val errorCode: String,
    override val message: String,
) : ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "USER001", "해당 유저를 찾을 수 없습니다."),
}
