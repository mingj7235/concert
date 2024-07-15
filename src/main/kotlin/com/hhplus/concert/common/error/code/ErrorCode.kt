package com.hhplus.concert.common.error.code

import org.springframework.http.HttpStatus

interface ErrorCode {
    val httpStatus: HttpStatus

    val errorCode: String

    val message: String
}
