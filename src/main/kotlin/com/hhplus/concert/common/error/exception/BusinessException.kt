package com.hhplus.concert.common.error.exception

import com.hhplus.concert.common.error.code.ErrorCode

open class BusinessException(
    val errorCode: ErrorCode,
) : RuntimeException() {
    class NotFound(
        errorCode: ErrorCode,
    ) : BusinessException(errorCode)

    class BadRequest(
        errorCode: ErrorCode,
    ) : BusinessException(errorCode)

    class Duplication(
        errorCode: ErrorCode,
    ) : BusinessException(errorCode)
}
