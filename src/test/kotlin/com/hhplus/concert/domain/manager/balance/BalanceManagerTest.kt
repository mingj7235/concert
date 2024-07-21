package com.hhplus.concert.domain.manager.balance

import com.hhplus.concert.business.domain.entity.Balance
import com.hhplus.concert.business.domain.entity.User
import com.hhplus.concert.business.domain.manager.BalanceManager
import com.hhplus.concert.business.domain.repository.BalanceRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.time.LocalDateTime

class BalanceManagerTest {
    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var balanceRepository: BalanceRepository

    @InjectMocks
    private lateinit var balanceManager: BalanceManager

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `기존 잔액이 있는 사용자의 잔액 충전 테스트`() {
        // Given
        val userId = 0L
        val initialAmount = 1000L
        val rechargeAmount = 500L

        val user = User("user")
        val existingBalance =
            Balance(
                user = user,
                amount = initialAmount,
                lastUpdatedAt = LocalDateTime.now(),
            )

        `when`(userRepository.findById(userId)).thenReturn(user)
        `when`(balanceRepository.findByUserId(userId)).thenReturn(existingBalance)

        // When
        val result = balanceManager.updateAmount(userId, rechargeAmount)

        // Then
        assertEquals(initialAmount + rechargeAmount, result.amount)
    }
}
