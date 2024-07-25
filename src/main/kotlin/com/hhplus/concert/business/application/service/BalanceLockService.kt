package com.hhplus.concert.business.application.service

import com.hhplus.concert.business.application.dto.BalanceServiceDto
import com.hhplus.concert.common.error.code.ErrorCode
import com.hhplus.concert.common.error.exception.BusinessException
import org.springframework.stereotype.Service

@Service
class BalanceLockService(
    private val balanceService: BalanceService,
) {
    /**
     * 잔액을 충전한다.
     * - 존재하는 user 인지 검증한다.
     * - user 는 존재하지만 balance 가 없다면 생성하고 충전한다.
     * - balance 가 존재한다면, 현재 금액에 요청된 금액을 더한다.
     */

    fun recharge(
        userId: Long,
        amount: Long,
    ): BalanceServiceDto.Detail {
        if (amount < 0) throw BusinessException.BadRequest(ErrorCode.Balance.BAD_RECHARGE_REQUEST)

        return balanceService.rechargeWithSimpleLock(
            userId = userId,
            amount = amount,
        )
    }
}
