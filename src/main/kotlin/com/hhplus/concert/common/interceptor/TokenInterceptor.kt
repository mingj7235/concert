package com.hhplus.concert.common.interceptor

import com.hhplus.concert.common.annotation.TokenRequired
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

/**
 * TODO : token manager 를 DI 받아 token 을 검증하도록 한다.
 */
@Component
class TokenInterceptor : HandlerInterceptor {
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

            val tokenId = request.getHeader("TOKEN-ID")
            if (tokenId == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "TOKEN-ID is missing")
                return false
            }

            if (!isValidToken(tokenId)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid TOKEN-ID")
                return false
            }

            // 로직을 완성한 후에는, 어노테이션과 resolver 를 추가하고, userId 를 꺼내서 컨트롤 할 수 있게끔 인터셉터를 변경한다.
            request.setAttribute("validatedTokenId", tokenId)
        }
        return true
    }

    /**
     * 토큰을 검증하는 로직을 넣는다.
     * 일단은 true 를 리턴한다.
     */
    private fun isValidToken(tokenId: String): Boolean {
        // 토큰 유효성 검사 로직 구현
        return true // 임시 구현
    }
}
