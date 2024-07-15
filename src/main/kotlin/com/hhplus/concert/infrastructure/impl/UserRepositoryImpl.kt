package com.hhplus.concert.infrastructure.impl

import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.exception.error.UserException
import com.hhplus.concert.infrastructure.entity.User
import com.hhplus.concert.infrastructure.jpa.UserJpaRepository
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {
    override fun findById(userId: Long): User =
        userJpaRepository.findById(userId).orElseThrow {
            UserException.UserNotFound()
        }
}
