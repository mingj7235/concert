package com.hhplus.concert.domain.repository

import com.hhplus.concert.infra.entity.Balance

interface BalanceRepository {
    fun findByUserId(userId: Long): Balance?

    fun save(balance: Balance): Balance
}
