package com.hhplus.concert.business.application.service

import com.hhplus.concert.business.application.dto.BalanceServiceDto
import com.hhplus.concert.business.domain.manager.BalanceManager
import com.hhplus.concert.common.error.code.ErrorCode
import com.hhplus.concert.common.error.exception.BusinessException
import org.springframework.orm.ObjectOptimisticLockingFailureException
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
        if (amount < 0) throw BusinessException.BadRequest(ErrorCode.Balance.BAD_RECHARGE_REQUEST)

        return runCatching {
            val rechargedBalance = balanceManager.updateAmount(userId = userId, amount = amount)
            BalanceServiceDto.Detail(
                userId = userId,
                currentAmount = rechargedBalance.amount,
            )
        }.getOrElse { exception ->
            when (exception) {
                is ObjectOptimisticLockingFailureException ->
                    throw BusinessException.BadRequest(ErrorCode.Balance.CONCURRENT_MODIFICATION)
                else -> throw exception
            }
        }
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
