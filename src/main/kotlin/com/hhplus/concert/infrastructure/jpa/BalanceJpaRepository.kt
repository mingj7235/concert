package com.hhplus.concert.infrastructure.jpa

import com.hhplus.concert.business.domain.entity.Balance
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface BalanceJpaRepository : JpaRepository<Balance, Long> {
    @Query("select b from Balance b where b.user.id = :userId")
    fun findByUserId(userId: Long): Balance?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Balance b WHERE b.user.id = :userId")
    fun findByUserIdWithPessimisticLock(userId: Long): Balance?
}
