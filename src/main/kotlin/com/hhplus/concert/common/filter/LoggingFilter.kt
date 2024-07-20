package com.hhplus.concert.common.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper

@Component
class LoggingFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestWrapper = ContentCachingRequestWrapper(request)
        val responseWrapper = ContentCachingResponseWrapper(response)

        logger.info(getRequestLog(requestWrapper))
        filterChain.doFilter(request, responseWrapper)
        logger.info(getResponseLog(responseWrapper))
    }

    private fun getRequestLog(request: ContentCachingRequestWrapper): String {
        val requestBody = String(request.contentAsByteArray)
        return """
            |=== REQUEST ===
            |Method: ${request.method}
            |URL: ${request.requestURL}
            |Headers: ${getHeadersAsString(request)}
            |Body: $requestBody
            |================
            """.trimMargin()
    }

    private fun getResponseLog(response: ContentCachingResponseWrapper): String {
        val responseBody = String(response.contentAsByteArray)
        return """
            |=== RESPONSE ===
            |Status: ${response.status}
            |Headers: ${getHeadersAsString(response)}
            |Body: $responseBody
            |=================
            """.trimMargin()
    }

    private fun getHeadersAsString(request: HttpServletRequest): String =
        request.headerNames.toList().joinToString(", ") {
            "$it: ${request.getHeader(it)}"
        }

    private fun getHeadersAsString(response: HttpServletResponse): String =
        response.headerNames.joinToString(", ") {
            "$it: ${response.getHeader(it)}"
        }
}
