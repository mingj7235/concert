package com.hhplus.concert.application.facade

import com.hhplus.concert.application.dto.BalanceServiceDto
import com.hhplus.concert.domain.manager.balance.BalanceManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BalanceService(
    private val balanceManager: BalanceManager,
) {
    /**
     * 잔액을 충전한다.
     * - 존재하는 user 인지 검증한다.
     * - user 는 존재하지만 balance 가 없다면 생성하고 충전한다.
     * - balance 가 존재한다면, 현재 금액에 요청된 금액을 더한다.
     */
    @Transactional
    fun recharge(
        userId: Long,
        amount: Long,
    ): BalanceServiceDto.Detail {
        val rechargedBalance =
            balanceManager.recharge(
                userId = userId,
                amount = amount,
            )

        return BalanceServiceDto.Detail(
            userId = userId,
            currentAmount = rechargedBalance.amount,
        )
    }

    /**
     * 잔액을 조회한다.
     */
    fun getBalanceByUserId(userId: Long): BalanceServiceDto.Detail {
        val balance = balanceManager.getBalanceByUserId(userId)
        return BalanceServiceDto.Detail(
            userId = userId,
            currentAmount = balance.amount,
        )
    }
}
