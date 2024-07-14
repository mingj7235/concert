package com.hhplus.concert.infra.impl

import com.hhplus.concert.common.exception.error.UserException
import com.hhplus.concert.domain.repository.UserRepository
import com.hhplus.concert.infra.entity.User
import com.hhplus.concert.infra.jpa.UserJpaRepository
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
