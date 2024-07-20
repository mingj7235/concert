package com.hhplus.concert.application.facade.integration

import com.hhplus.concert.business.application.service.BalanceService
import com.hhplus.concert.business.domain.repository.BalanceRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.error.exception.BusinessException
import com.hhplus.concert.infrastructure.entity.Balance
import com.hhplus.concert.infrastructure.entity.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@Transactional
class BalanceServiceIntegrationTest {
    @Autowired
    private lateinit var balanceService: BalanceService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var balanceRepository: BalanceRepository

    @Test
    fun `잔액 충전 - 새로운 Balance 생성`() {
        // given
        val user = userRepository.save(User(name = "Test User"))

        // when
        val result = balanceService.recharge(user.id, 10000)

        // then
        assertEquals(user.id, result.userId)
        assertEquals(10000L, result.currentAmount)

        val savedBalance = balanceRepository.findByUserId(user.id)
        assertNotNull(savedBalance)
        assertEquals(10000L, savedBalance?.amount)
    }

    @Test
    fun `잔액 충전 - 기존 Balance 업데이트`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        balanceRepository.save(Balance(user = user, amount = 5000, lastUpdatedAt = LocalDateTime.now()))

        // when
        val result = balanceService.recharge(user.id, 3000)

        // then
        assertEquals(user.id, result.userId)
        assertEquals(8000L, result.currentAmount)

        val updatedBalance = balanceRepository.findByUserId(user.id)
        assertNotNull(updatedBalance)
        assertEquals(8000L, updatedBalance?.amount)
    }

    @Test
    fun `잔액 충전 - 존재하지 않는 사용자`() {
        // given
        val nonExistentUserId = 9999L

        // when & then
        assertThrows<BusinessException.NotFound> {
            balanceService.recharge(nonExistentUserId, 10000)
        }
    }

    @Test
    fun `잔액 충전 - 0원 충전`() {
        // given
        val user = userRepository.save(User(name = "Test User"))

        // when
        val result = balanceService.recharge(user.id, 0)

        // then
        assertEquals(user.id, result.userId)
        assertEquals(0L, result.currentAmount)

        val savedBalance = balanceRepository.findByUserId(user.id)
        assertNotNull(savedBalance)
        assertEquals(0L, savedBalance?.amount)
    }

    @Test
    fun `잔액 충전 - 대량 충전`() {
        // given
        val user = userRepository.save(User(name = "Test User"))

        // when
        val result = balanceService.recharge(user.id, 1_000_000_000) // 10억원

        // then
        assertEquals(user.id, result.userId)
        assertEquals(1_000_000_000L, result.currentAmount)

        val savedBalance = balanceRepository.findByUserId(user.id)
        assertNotNull(savedBalance)
        assertEquals(1_000_000_000L, savedBalance?.amount)
    }

    @Test
    fun `잔액 충전 - 여러 번 충전`() {
        // given
        val user = userRepository.save(User(name = "Test User"))

        // when
        balanceService.recharge(user.id, 1000)
        balanceService.recharge(user.id, 2000)
        val result = balanceService.recharge(user.id, 3000)

        // then
        assertEquals(user.id, result.userId)
        assertEquals(6000L, result.currentAmount)

        val savedBalance = balanceRepository.findByUserId(user.id)
        assertNotNull(savedBalance)
        assertEquals(6000L, savedBalance?.amount)
    }
}
