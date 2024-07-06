package com.hhplus.concert.infra.jpa

import com.hhplus.concert.domain.entity.Queue
import org.springframework.data.jpa.repository.JpaRepository

interface QueueJpaRepository : JpaRepository<Queue, Long>
