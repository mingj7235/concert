package com.hhplus.concert.infra.impl

import com.hhplus.concert.application.repository.QueueRepository
import com.hhplus.concert.infra.jpa.QueueJpaRepository
import org.springframework.stereotype.Repository

@Repository
class QueueRepositoryImpl(
    private val queueJpaRepository: QueueJpaRepository,
) : QueueRepository {
}
