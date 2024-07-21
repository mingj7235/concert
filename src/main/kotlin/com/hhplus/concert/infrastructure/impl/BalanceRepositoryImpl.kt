package com.hhplus.concert.infrastructure.impl

import com.hhplus.concert.business.domain.entity.Balance
import com.hhplus.concert.business.domain.repository.BalanceRepository
import com.hhplus.concert.infrastructure.jpa.BalanceJpaRepository
import org.springframework.stereotype.Repository

@Repository
class BalanceRepositoryImpl(
    private val balanceJpaRepository: BalanceJpaRepository,
) : BalanceRepository {
    override fun findByUserId(userId: Long): Balance? = balanceJpaRepository.findByUserId(userId)

    override fun findByUserIdWithLock(id: Long): Balance? = balanceJpaRepository.findByUserIdWithLock(id)

    override fun save(balance: Balance): Balance = balanceJpaRepository.save(balance)
}
