package com.hhplus.concert.infrastructure.jpa

import com.hhplus.concert.business.domain.entity.Balance
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BalanceJpaRepository : JpaRepository<Balance, Long> {
    @Query("select b from Balance b where b.user.id = :userId")
    fun findByUserId(userId: Long): Balance?
}
