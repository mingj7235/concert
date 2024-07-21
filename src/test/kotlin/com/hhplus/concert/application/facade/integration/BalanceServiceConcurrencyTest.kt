package com.hhplus.concert.application.facade.integration

import com.hhplus.concert.business.application.service.BalanceService
import com.hhplus.concert.business.domain.entity.Balance
import com.hhplus.concert.business.domain.entity.User
import com.hhplus.concert.business.domain.repository.BalanceRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class BalanceServiceConcurrencyTest {
    @Autowired
    private lateinit var balanceService: BalanceService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var balanceRepository: BalanceRepository

    @Test
    fun `10개의 동시 충전 요청 중 하나만 성공해야 한다`() {
        // Given
        val user = userRepository.save(User(name = "Test User"))
        balanceRepository.save(
            Balance(
                user = user,
                amount = 1000,
                lastUpdatedAt = LocalDateTime.now(),
            ),
        )

        val threadCount = 10
        val executorService = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val rechargeAmount = 100L

        val successfulRecharges = AtomicInteger(0)
        val failedRecharges = AtomicInteger(0)

        // When
        repeat(threadCount) {
            executorService.submit {
                try {
                    runCatching {
                        balanceService.recharge(user.id, rechargeAmount)
                    }.onSuccess {
                        successfulRecharges.incrementAndGet()
                    }.onFailure {
                        failedRecharges.incrementAndGet()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()

        // Then
        val finalBalance = balanceRepository.findByUserId(user.id)

        assertEquals(1, successfulRecharges.get(), "1개의 충전만 성공해야 합니다.")
        assertEquals(9, failedRecharges.get(), "9개의 충전은 실패해야 합니다.")
        assertEquals(1100L, finalBalance?.amount, "최종 잔액은 1100이어야 합니다.")
    }
}
