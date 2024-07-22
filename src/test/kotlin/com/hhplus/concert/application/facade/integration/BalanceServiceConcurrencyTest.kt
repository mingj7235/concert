package com.hhplus.concert.application.facade.integration

import com.hhplus.concert.business.application.service.BalanceService
import com.hhplus.concert.business.domain.entity.Balance
import com.hhplus.concert.business.domain.entity.User
import com.hhplus.concert.business.domain.repository.BalanceRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.assertEquals

@SpringBootTest
@TestPropertySource(locations = ["classpath:application.yml"])
class BalanceServiceConcurrencyTest {
    @Autowired
    private lateinit var balanceService: BalanceService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var balanceRepository: BalanceRepository

    @Test
    fun `10개의 동시 충전 요청이 오면 10개가 성공적으로 모두 반영되어 충전되어야 한다`() {
        // Given
        val user = userRepository.save(User(name = "Test User"))
        balanceRepository.save(
            Balance(
                user = user,
                amount = 1000,
                lastUpdatedAt = LocalDateTime.now(),
            ),
        )

        val threadCount = 1000
        val executorService = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val rechargeAmount = 100L

        // When
        repeat(threadCount) {
            executorService.submit {
                try {
                    runCatching {
                        balanceService.recharge(user.id, rechargeAmount)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()

        // Then

        val finalBalance = balanceRepository.findByUserId(user.id)
        assertEquals(2000L, finalBalance?.amount, "최종 잔액은 2000이어야 합니다.")

        Assertions.assertEquals(1100L, finalBalance?.amount, "최종 잔액은 1100이어야 합니다.")
    }
}
