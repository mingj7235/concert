package com.hhplus.concert.business.domain.manager

import com.hhplus.concert.business.domain.entity.Balance
import com.hhplus.concert.business.domain.repository.BalanceRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.error.code.ErrorCode
import com.hhplus.concert.common.error.exception.BusinessException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class BalanceManager(
    private val userRepository: UserRepository,
    private val balanceRepository: BalanceRepository,
) {
    @Transactional
    fun updateAmount(
        userId: Long,
        amount: Long,
    ): Balance {
        val user = userRepository.findById(userId) ?: throw BusinessException.NotFound(ErrorCode.User.NOT_FOUND)
        return balanceRepository.findByUserId(user.id)?.apply {
            updateAmount(amount)
        } ?: balanceRepository.save(
            Balance(
                user = user,
                amount = amount,
                lastUpdatedAt = LocalDateTime.now(),
            ),
        )
    }

    fun getBalanceByUserId(userId: Long): Balance =
        balanceRepository.findByUserId(userId) ?: throw BusinessException.NotFound(ErrorCode.Balance.NOT_FOUND)
}
