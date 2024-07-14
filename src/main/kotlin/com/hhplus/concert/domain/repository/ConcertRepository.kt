package com.hhplus.concert.domain.repository

import com.hhplus.concert.infra.entity.Concert

interface ConcertRepository {
    fun findById(concertId: Long): Concert

    fun findAll(): List<Concert>
}
