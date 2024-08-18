package com.hhplus.concert.common.error.code

import org.springframework.http.HttpStatus

sealed interface ErrorCode {
    val httpStatus: HttpStatus
    val errorCode: String
    val message: String

    enum class Common(
        override val httpStatus: HttpStatus,
        override val errorCode: String,
        override val message: String,
    ) : ErrorCode {
        NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON001", "찾을 수 없습니다."),
        BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON002", "잘못된 요청입니다."),
        INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON003", "서버에 문제가 발생했습니다."),
    }

    enum class Concert(
        override val httpStatus: HttpStatus,
        override val errorCode: String,
        override val message: String,
    ) : ErrorCode {
        NOT_FOUND(HttpStatus.NOT_FOUND, "CONCERT001", "해당 콘서트를 찾을 수 없습니다."),
        UNAVAILABLE(HttpStatus.BAD_REQUEST, "CONCERT002", "해당 콘서트는 이용이 불가능합니다."),
        SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "CONCERT003", "해당 콘서트의 일정을 찾을 수 없습니다."),
    }

    enum class Balance(
        override val httpStatus: HttpStatus,
        override val errorCode: String,
        override val message: String,
    ) : ErrorCode {
        NOT_FOUND(HttpStatus.NOT_FOUND, "BALANCE001", "해당 잔액 정보를 찾을 수 없습니다."),
        BAD_RECHARGE_REQUEST(HttpStatus.BAD_REQUEST, "BALANCE002", "잘못된 잔액 충전 요청입니다."),
    }

    enum class Payment(
        override val httpStatus: HttpStatus,
        override val errorCode: String,
        override val message: String,
    ) : ErrorCode {
        NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT001", "결제 정보를 찾을 수 없습니다."),
        BAD_REQUEST(HttpStatus.BAD_REQUEST, "PAYMENT002", "잘못된 결제 정보 요청입니다."),
    }

    enum class User(
        override val httpStatus: HttpStatus,
        override val errorCode: String,
        override val message: String,
    ) : ErrorCode {
        NOT_FOUND(HttpStatus.NOT_FOUND, "USER001", "해당 유저를 찾을 수 없습니다."),
    }

    enum class Queue(
        override val httpStatus: HttpStatus,
        override val errorCode: String,
        override val message: String,
    ) : ErrorCode {
        NOT_FOUND(HttpStatus.NOT_FOUND, "QUEUE001", "해당 대기열을 찾을 수 없습니다."),
        NOT_ALLOWED(HttpStatus.BAD_REQUEST, "QUEUE002", "해당 대기열은 허용되지 않습니다."),
    }

    enum class Event(
        override val httpStatus: HttpStatus,
        override val errorCode: String,
        override val message: String,
    ) : ErrorCode {
        BAD_REQUEST(HttpStatus.BAD_REQUEST, "EVENT001", "잘못된 이벤트 요청입니다."),
        NOT_FOUND(HttpStatus.NOT_FOUND, "EVENT002", "해당 이벤트를 찾을 수 없습니다."),
    }
}
