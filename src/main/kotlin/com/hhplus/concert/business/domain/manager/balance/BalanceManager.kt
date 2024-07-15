package com.hhplus.concert.business.domain.manager.balance

import com.hhplus.concert.business.domain.repository.BalanceRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.exception.error.BalanceException
import com.hhplus.concert.infrastructure.entity.Balance
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class BalanceManager(
    private val userRepository: UserRepository,
    private val balanceRepository: BalanceRepository,
) {
    fun recharge(
        userId: Long,
        amount: Long,
    ): Balance {
        val user = userRepository.findById(userId)
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

    fun getBalanceByUserId(userId: Long): Balance = balanceRepository.findByUserId(userId) ?: throw BalanceException.BalanceNotFound()
}
