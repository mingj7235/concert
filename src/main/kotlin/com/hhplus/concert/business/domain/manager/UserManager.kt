package com.hhplus.concert.business.domain.manager

import com.hhplus.concert.business.domain.entity.User
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.error.code.ErrorCode
import com.hhplus.concert.common.error.exception.BusinessException
import org.springframework.stereotype.Component

@Component
class UserManager(
    private val userRepository: UserRepository,
) {
    fun findById(userId: Long): User = userRepository.findById(userId) ?: throw BusinessException.NotFound(ErrorCode.User.NOT_FOUND)
}
