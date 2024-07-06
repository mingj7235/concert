package com.hhplus.concert.application.manager

import com.hhplus.concert.application.repository.QueueRepository
import org.springframework.stereotype.Component

@Component
class QueueManager(
    private val queueRepository: QueueRepository,
)
