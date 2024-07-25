package com.hhplus.concert.business.domain.manager.balance

import com.hhplus.concert.business.domain.entity.Balance
import com.hhplus.concert.common.annotation.DistributedSimpleLock
import org.springframework.stereotype.Component

@Component
class BalanceLockManager(
    private val balanceManager: BalanceManager,
) {
    @DistributedSimpleLock(
        key = "'user:' + #userId",
        waitTime = 5,
        leaseTime = 10,
    )
    fun rechargeWithLock(
        userId: Long,
        amount: Long,
    ): Balance =
        balanceManager.updateAmount(
            userId = userId,
            amount = amount,
        )
}
