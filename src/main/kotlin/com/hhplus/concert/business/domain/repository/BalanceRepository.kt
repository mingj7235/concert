package com.hhplus.concert.business.domain.repository

import com.hhplus.concert.business.domain.entity.Balance

interface BalanceRepository {
    fun findByUserId(userId: Long): Balance?

    fun save(balance: Balance): Balance
}
