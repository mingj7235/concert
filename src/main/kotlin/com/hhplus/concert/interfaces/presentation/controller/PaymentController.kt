package com.hhplus.concert.interfaces.presentation.controller

import com.hhplus.concert.business.application.service.PaymentService
import com.hhplus.concert.common.annotation.TokenRequired
import com.hhplus.concert.common.annotation.ValidatedToken
import com.hhplus.concert.interfaces.presentation.request.PaymentRequest
import com.hhplus.concert.interfaces.presentation.response.PaymentResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/payment")
class PaymentController(
    private val paymentService: PaymentService,
) {
    /**
     * 결제를 진행한다.
     * 1. reservation 의 user 와, payment 를 요청하는 user 가 일치하는지 검증
     * 2. payment 수행하고 paymentHistory 에 저장
     * 3. reservation 상태 변경
     * 4. 토큰의 상태 변경 -> completed
     */
    @TokenRequired
    @PostMapping("/payments/users/{userId}")
    fun executePayment(
        @ValidatedToken token: String,
        @PathVariable userId: Long,
        @RequestBody paymentRequest: PaymentRequest.Detail,
    ): List<PaymentResponse.Result> =
        paymentService
            .executePayment(
                token = token,
                userId = userId,
                reservationIds = paymentRequest.reservationIds,
            ).map {
                PaymentResponse.Result.from(it)
            }
}
