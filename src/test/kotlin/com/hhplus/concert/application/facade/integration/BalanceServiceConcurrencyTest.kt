package com.hhplus.concert.application.facade.integration

import com.hhplus.concert.business.application.service.BalanceService
import com.hhplus.concert.business.domain.entity.Balance
import com.hhplus.concert.business.domain.entity.User
import com.hhplus.concert.business.domain.repository.BalanceRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

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
    fun `SpinLock을 사용할 때 10회 요청이 동시에 들어오면 1회 충전만 된다 하지만 실패하지는 않는다`() {
        val numberOfThreads = 10
        val rechargeAmount = 1000L

        val executor = Executors.newFixedThreadPool(numberOfThreads)
        val successfulRecharges = AtomicInteger(0)
        val failedRecharges = AtomicInteger(0)

        val totalTime = measureTimeMillis {
            val futures = (1..numberOfThreads).map {
                executor.submit {
                    try {
                        balanceService.recharge(testUser.id, rechargeAmount)
                        successfulRecharges.incrementAndGet()
                    } catch (e: Exception) {
                        failedRecharges.incrementAndGet()
                    }
                }
            }

            futures.forEach { it.get() } // 모든 작업이 완료될 때까지 대기
        }

        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.MINUTES)

        // 결과 검증
        val finalBalance = balanceRepository.findByUserId(testUser.id)
        assertNotNull(finalBalance, "잔액이 존재해야 합니다")
        assertEquals(rechargeAmount, finalBalance!!.amount, "최종 잔액이 예상 금액과 일치해야 합니다")
        assertEquals(10, successfulRecharges.get(), "모든 충전 시도가 성공해야 합니다")
        assertEquals(0, failedRecharges.get(), "실패한 충전이 없어야 합니다")

        println("성공한 충전 횟수: ${successfulRecharges.get()}")
        println("실패한 충전 횟수: ${failedRecharges.get()}")
        println("최종 잔액: ${finalBalance.amount}")
        println("총 실행 시간: $totalTime ms")
        println("평균 충전 시간: ${totalTime.toDouble() / numberOfThreads} ms")
    }
}