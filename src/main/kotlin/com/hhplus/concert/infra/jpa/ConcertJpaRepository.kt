package com.hhplus.concert.infra.jpa

import com.hhplus.concert.infra.entity.Concert
import org.springframework.data.jpa.repository.JpaRepository

interface ConcertJpaRepository : JpaRepository<Concert, Long>
