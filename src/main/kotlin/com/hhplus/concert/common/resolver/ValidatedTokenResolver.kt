package com.hhplus.concert.common.resolver

import com.hhplus.concert.common.annotation.ValidatedToken
import com.hhplus.concert.common.constants.TokenConstants.VALIDATED_TOKEN
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class ValidatedTokenResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean = parameter.hasParameterAnnotation(ValidatedToken::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any? = webRequest.getAttribute(VALIDATED_TOKEN, RequestAttributes.SCOPE_REQUEST)
}
