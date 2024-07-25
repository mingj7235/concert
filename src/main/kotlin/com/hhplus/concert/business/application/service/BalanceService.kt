package com.hhplus.concert.business.application.service

import com.hhplus.concert.business.application.dto.BalanceServiceDto
import com.hhplus.concert.business.domain.manager.BalanceManager
import com.hhplus.concert.common.annotation.DistributedSimpleLock
import org.springframework.stereotype.Service

@Service
class BalanceService(
    private val balanceManager: BalanceManager,
) {
    @DistributedSimpleLock(
        key = "'user:' + #userId",
        waitTime = 5,
        leaseTime = 10,
    )
    fun rechargeWithSimpleLock(
        userId: Long,
        amount: Long,
    ): BalanceServiceDto.Detail {
        val rechargedBalance =
            balanceManager.updateAmount(
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
