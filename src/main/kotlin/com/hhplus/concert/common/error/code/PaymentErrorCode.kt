package com.hhplus.concert.common.error.code

import org.springframework.http.HttpStatus

enum class PaymentErrorCode(
    override val httpStatus: HttpStatus,
    override val errorCode: String,
    override val message: String,
) : ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "PAYMENT001", "잘못된 결제 정보 요청입니다."),
}
