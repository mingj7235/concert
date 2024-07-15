package com.hhplus.concert.interfaces.presentation.controller

import com.hhplus.concert.business.application.service.BalanceService
import com.hhplus.concert.interfaces.presentation.request.BalanceRequest
import com.hhplus.concert.interfaces.presentation.response.BalanceResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/balance")
class BalanceController(
    private val balanceService: BalanceService,
) {
    // 잔액을 충전한다.
    @PostMapping("/users/{userId}/recharge")
    fun recharge(
        @PathVariable userId: Long,
        @RequestBody rechargeRequest: BalanceRequest.Recharge,
    ): BalanceResponse.Detail =
        BalanceResponse.Detail.from(
            balanceService.recharge(userId, rechargeRequest.amount),
        )

    // 잔액을 조회한다.
    @GetMapping("/users/{userId}")
    fun getBalance(
        @PathVariable userId: Long,
    ): BalanceResponse.Detail =
        BalanceResponse.Detail.from(
            balanceService.getBalanceByUserId(userId),
        )
}
