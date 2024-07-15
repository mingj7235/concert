package com.hhplus.concert.common.error.code

import org.springframework.http.HttpStatus

enum class BalanceErrorCode(
    override val httpStatus: HttpStatus,
    override val errorCode: String,
    override val message: String,
) : ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "BALANCE001", "해당 잔액 정보를 찾을 수 없습니다."),
}
