package com.hhplus.concert.business.domain.repository

import com.hhplus.concert.infrastructure.entity.Concert

interface ConcertRepository {
    fun findById(concertId: Long): Concert

    fun findAll(): List<Concert>
}
