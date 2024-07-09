package com.hhplus.concert.infra.impl

import com.hhplus.concert.common.exception.error.ConcertException
import com.hhplus.concert.domain.repository.ConcertRepository
import com.hhplus.concert.infra.entity.Concert
import com.hhplus.concert.infra.jpa.ConcertJpaRepository
import org.springframework.stereotype.Repository

@Repository
class ConcertRepositoryImpl(
    private val concertJpaRepository: ConcertJpaRepository,
) : ConcertRepository {
    override fun findById(concertId: Long): Concert =
        concertJpaRepository.findById(concertId).orElseThrow {
            ConcertException.NotFound()
        }

    override fun findAll(): List<Concert> = concertJpaRepository.findAll()
}
