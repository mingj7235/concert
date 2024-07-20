package com.hhplus.concert.business.domain.repository

import com.hhplus.concert.infrastructure.entity.User

interface UserRepository {
    fun save(user: User): User

    fun findById(userId: Long): User?
}
