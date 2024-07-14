package com.hhplus.concert.infra.jpa

import com.hhplus.concert.infra.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<User, Long>
