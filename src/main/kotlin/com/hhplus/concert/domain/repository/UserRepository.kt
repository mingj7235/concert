package com.hhplus.concert.domain.repository

import com.hhplus.concert.infra.entity.User

interface UserRepository {
    fun findById(userId: Long): User
}
