package com.hhplus.concert.infra.impl

import com.hhplus.concert.domain.repository.BalanceRepository
import com.hhplus.concert.infra.entity.Balance
import com.hhplus.concert.infra.jpa.BalanceJpaRepository
import org.springframework.stereotype.Repository

@Repository
class BalanceRepositoryImpl(
    private val balanceJpaRepository: BalanceJpaRepository,
) : BalanceRepository {
    override fun findByUserId(userId: Long): Balance? = balanceJpaRepository.findByUserId(userId)

    override fun save(balance: Balance): Balance = balanceJpaRepository.save(balance)
}
