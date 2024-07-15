package com.hhplus.concert.business.domain.manager.user

import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.infrastructure.entity.User
import org.springframework.stereotype.Component

@Component
class UserManager(
    private val userRepository: UserRepository,
) {
    fun findById(userId: Long): User = userRepository.findById(userId)
}
