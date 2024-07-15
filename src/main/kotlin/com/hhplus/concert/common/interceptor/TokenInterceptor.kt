package com.hhplus.concert.common.interceptor

import com.hhplus.concert.common.annotation.TokenRequired
import com.hhplus.concert.common.util.JwtUtil
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class TokenInterceptor(
    private val jwtUtil: JwtUtil,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler is HandlerMethod) {
            val requireToken =
                handler.hasMethodAnnotation(TokenRequired::class.java) ||
                    handler.beanType.isAnnotationPresent(TokenRequired::class.java)

            if (!requireToken) {
                return true
            }

            val token = request.getHeader("QUEUE-TOKEN")
            if (token == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "QUEUE-TOKEN is missing")
                return false
            }

            if (!isValidToken(token)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid QUEUE-TOKEN")
                return false
            }

            request.setAttribute("validatedToken", token)
        }
        return true
    }

    private fun isValidToken(token: String): Boolean = jwtUtil.validateToken(token)
}
