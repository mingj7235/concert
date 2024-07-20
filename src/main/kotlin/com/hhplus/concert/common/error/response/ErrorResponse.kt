package com.hhplus.concert.common.error.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.hhplus.concert.common.error.code.ErrorCode
import jakarta.validation.ConstraintViolation
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse private constructor(
    val status: Int = 0,
    val error: String? = null,
    val code: String? = null,
    val message: String? = null,
    val errors: Map<String, String>? = null,
    val path: String? = null,
) {
    private constructor(
        errorCode: ErrorCode,
        requestURI: String,
    ) : this(
        status = errorCode.httpStatus.value(),
        error = errorCode.httpStatus.reasonPhrase,
        code = errorCode.errorCode,
        message = errorCode.message,
        path = requestURI,
    )

    private constructor(
        errors: Set<ConstraintViolation<*>>,
        requestURI: String,
    ) : this(
        status = HttpStatus.BAD_REQUEST.value(),
        error = HttpStatus.BAD_REQUEST.reasonPhrase,
        path = requestURI,
        errors = errors.associateBy({ it.propertyPath.toString() }, { it.message }),
    )

    companion object {
        fun toResponseEntity(
            errorCode: ErrorCode,
            requestURI: String,
        ): ResponseEntity<ErrorResponse> =
            ResponseEntity
                .status(errorCode.httpStatus)
                .body(ErrorResponse(errorCode, requestURI))

        fun toResponseEntity(
            errors: Set<ConstraintViolation<*>>,
            requestURI: String,
        ): ResponseEntity<ErrorResponse> =
            ResponseEntity
                .badRequest()
                .body(ErrorResponse(errors, requestURI))
    }
}
