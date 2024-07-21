package com.hhplus.concert.infrastructure.jpa

import com.hhplus.concert.business.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<User, Long>
