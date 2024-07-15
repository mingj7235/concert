package com.hhplus.concert.infrastructure.jpa

import com.hhplus.concert.infrastructure.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<User, Long>
