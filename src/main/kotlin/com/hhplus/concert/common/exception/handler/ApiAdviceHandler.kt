package com.hhplus.concert.common.exception.handler

import com.hhplus.concert.common.exception.error.ConcertException
import com.hhplus.concert.common.exception.error.QueueException
import com.hhplus.concert.common.exception.error.UserException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class ApiAdviceHandler : ResponseEntityExceptionHandler() {
    data class ErrorResponse(
        val code: Int,
        val message: String,
    )

    @ExceptionHandler(UserException::class)
    fun handleUserException(e: Exception): ResponseEntity<ErrorResponse> =
        ResponseEntity(
            ErrorResponse(HttpStatus.BAD_REQUEST.value(), e.message.orEmpty()),
            HttpStatus.BAD_REQUEST,
        )

    @ExceptionHandler(QueueException::class)
    fun handleQueueException(e: Exception): ResponseEntity<ErrorResponse> =
        ResponseEntity(
            ErrorResponse(HttpStatus.BAD_REQUEST.value(), e.message.orEmpty()),
            HttpStatus.BAD_REQUEST,
        )

    @ExceptionHandler(ConcertException::class)
    fun handleConcertException(e: Exception): ResponseEntity<ErrorResponse> =
        ResponseEntity(
            ErrorResponse(HttpStatus.BAD_REQUEST.value(), e.message.orEmpty()),
            HttpStatus.BAD_REQUEST,
        )
}
