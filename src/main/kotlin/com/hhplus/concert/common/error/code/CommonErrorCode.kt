package com.hhplus.concert.common.error.code

import org.springframework.http.HttpStatus

enum class CommonErrorCode(
    override val httpStatus: HttpStatus,
    override val errorCode: String,
    override val message: String,
) : ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON001", "찾을 수 없습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON002", "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON003", "서버에 문제가 발생했습니다."),
}
