package com.hhplus.concert.infra.jpa

import com.hhplus.concert.infra.entity.Balance
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BalanceJpaRepository : JpaRepository<Balance, Long> {
    @Query("select b from Balance b where b.user = :userId")
    fun findByUserId(userId: Long): Balance?
}
