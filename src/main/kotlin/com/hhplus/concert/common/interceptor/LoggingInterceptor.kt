package com.hhplus.concert.common.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class LoggingInterceptor : HandlerInterceptor {
    private val logger = LoggerFactory.getLogger(LoggingInterceptor::class.java)

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        logger.info("Request - URL: ${request.requestURL}, Method: ${request.method}")
        logger.info("Request Headers: ${request.headerNames.toList().associateWith { request.getHeader(it) }}")
        logger.info("Request Parameters: ${request.parameterMap}")
        return true
    }
}
