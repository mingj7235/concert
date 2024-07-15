package com.hhplus.concert.infrastructure.jpa

import com.hhplus.concert.infrastructure.entity.Concert
import org.springframework.data.jpa.repository.JpaRepository

interface ConcertJpaRepository : JpaRepository<Concert, Long>
