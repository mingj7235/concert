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

@SpringBootTest
class BalanceServiceConcurrencyTest {
    @Autowired
    private lateinit var balanceService: BalanceService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var balanceRepository: BalanceRepository

    /**
     * 동시에 많은 요청이 들어온다면 낙관적락은 적합하지 않다.
     */
    @Test
    fun `100 회의 동시 충전 요청이 오면 1회만 성공해야 한다`() {
        // Given
        val user = userRepository.save(User(name = "Test User"))
        balanceRepository.save(
            Balance(
                user = user,
                amount = 100,
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
        assertEquals(200L, finalBalance?.amount, "최종 잔액은 200이어야 합니다.")
    }
}
