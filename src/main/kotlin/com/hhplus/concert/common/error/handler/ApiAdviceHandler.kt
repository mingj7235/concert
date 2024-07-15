package com.hhplus.concert.common.error.handler

import com.hhplus.concert.common.error.code.CommonErrorCode
import com.hhplus.concert.common.error.exception.BusinessException
import com.hhplus.concert.common.error.response.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class ApiAdviceHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(
        e: ConstraintViolationException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.error(
            "[Constraint Violation Exception] " +
                "- http status code : ${CommonErrorCode.BAD_REQUEST.httpStatus}" +
                "- error message : ${e.message}",
        )
        return ErrorResponse.toResponseEntity(e.constraintViolations, request.requestURI)
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        e: BusinessException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.error(
            "[Business Exception] " +
                "- http status code : ${e.errorCode.httpStatus}" +
                "- custom error code : ${e.errorCode.errorCode} " +
                "- message : ${e.errorCode.message}",
        )
        return ErrorResponse.toResponseEntity(e.errorCode, request.requestURI)
    }
}
