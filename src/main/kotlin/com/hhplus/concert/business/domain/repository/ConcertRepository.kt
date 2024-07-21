package com.hhplus.concert.business.domain.repository

import com.hhplus.concert.business.domain.entity.Concert

interface ConcertRepository {
    fun findById(concertId: Long): Concert?

    fun findAll(): List<Concert>

    fun save(concert: Concert): Concert

    fun saveAll(concerts: List<Concert>)
}
