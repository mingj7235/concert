package com.hhplus.concert.domain.manager.user

import com.hhplus.concert.domain.repository.UserRepository
import com.hhplus.concert.infra.entity.User
import org.springframework.stereotype.Component

@Component
class UserManager(
    private val userRepository: UserRepository,
) {
    fun findById(userId: Long): User = userRepository.findById(userId)
}
