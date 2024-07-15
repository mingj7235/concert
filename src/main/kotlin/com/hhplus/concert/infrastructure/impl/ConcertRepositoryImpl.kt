package com.hhplus.concert.infrastructure.impl

import com.hhplus.concert.business.domain.repository.ConcertRepository
import com.hhplus.concert.infrastructure.entity.Concert
import com.hhplus.concert.infrastructure.jpa.ConcertJpaRepository
import org.springframework.stereotype.Repository
import kotlin.jvm.optionals.getOrNull

@Repository
class ConcertRepositoryImpl(
    private val concertJpaRepository: ConcertJpaRepository,
) : ConcertRepository {
    override fun findById(concertId: Long): Concert? = concertJpaRepository.findById(concertId).getOrNull()

    override fun findAll(): List<Concert> = concertJpaRepository.findAll()
}
