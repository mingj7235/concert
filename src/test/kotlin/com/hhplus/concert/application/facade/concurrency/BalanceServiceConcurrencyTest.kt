package com.hhplus.concert.application.facade.concurrency

import com.hhplus.concert.business.application.service.BalanceService
import com.hhplus.concert.business.domain.entity.Balance
import com.hhplus.concert.business.domain.entity.User
import com.hhplus.concert.business.domain.repository.BalanceRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class BalanceServiceConcurrencyTest {
    @Autowired
    private lateinit var balanceService: BalanceService

    @Autowired
    private lateinit var balanceRepository: BalanceRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var testUser: User

    @BeforeEach
    fun setup() {
        // 테스트 사용자 생성
        testUser = userRepository.save(User(name = "Test User"))

        // 초기 잔액 설정
        balanceRepository.save(Balance(user = testUser, amount = 0, lastUpdatedAt = LocalDateTime.now()))
    }

    @Test
    fun `동시에 여러 충전 요청이 들어와도 한 번만 충전되어야 한다`() {
        // Given
        val userId = 1L
        val rechargeAmount = 500L
        val numberOfThreads = 1000

        // 초기 잔액 설정

        val executorService = Executors.newFixedThreadPool(numberOfThreads)
        val successfulRecharges = AtomicInteger(0)
        val failedRecharges = AtomicInteger(0)

        // When
        repeat(numberOfThreads) {
            executorService.submit {
                try {
                    balanceService.recharge(testUser.id, rechargeAmount)
                    successfulRecharges.incrementAndGet()
                } catch (e: Exception) {
                    failedRecharges.incrementAndGet()
                }
            }
        }

        executorService.shutdown()
        executorService.awaitTermination(10, TimeUnit.SECONDS)

        // Then
        val finalBalance = balanceRepository.findByUserId(userId)?.amount ?: 0L

        assertEquals(1, successfulRecharges.get(), "오직 한 번의 충전만 성공해야 한다.")
        assertEquals(999, failedRecharges.get(), "나머지는 모두 실패한다.")
        assertEquals(rechargeAmount, finalBalance, "최종 잔액은 초기 잔액 + 1회 충전액이어야 한다.")
    }
}
