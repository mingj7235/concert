package com.hhplus.concert.common.config

import com.hhplus.concert.common.resolver.ValidatedTokenResolver
import com.hhplus.concert.interfaces.presentation.interceptor.TokenInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val tokenInterceptor: TokenInterceptor,
    private val validatedTokenResolver: ValidatedTokenResolver,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(tokenInterceptor)
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(validatedTokenResolver)
    }
}
