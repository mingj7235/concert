package com.hhplus.concert.application.facade.concurrency

import com.hhplus.concert.business.application.dto.ReservationServiceDto
import com.hhplus.concert.business.application.service.ReservationService
import com.hhplus.concert.business.domain.entity.Concert
import com.hhplus.concert.business.domain.entity.ConcertSchedule
import com.hhplus.concert.business.domain.entity.Seat
import com.hhplus.concert.business.domain.entity.User
import com.hhplus.concert.business.domain.repository.ConcertRepository
import com.hhplus.concert.business.domain.repository.ConcertScheduleRepository
import com.hhplus.concert.business.domain.repository.ReservationRepository
import com.hhplus.concert.business.domain.repository.SeatRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.type.ConcertStatus
import com.hhplus.concert.common.type.SeatStatus
import com.hhplus.concert.infrastructure.redis.QueueRedisRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class ReservationServiceConcurrencyTest {
    @Autowired
    private lateinit var reservationService: ReservationService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var concertRepository: ConcertRepository

    @Autowired
    private lateinit var concertScheduleRepository: ConcertScheduleRepository

    @Autowired
    private lateinit var queueRedisRepository: QueueRedisRepository

    @Autowired
    private lateinit var reservationRepository: ReservationRepository

    @Autowired
    private lateinit var seatRepository: SeatRepository

    private var concertId: Long = 0
    private var scheduleId: Long = 0
    private var seatId: Long = 0
    private val userIds = mutableListOf<Long>()
    private val tokens = mutableListOf<String>()

    @BeforeEach
    fun setup() {
        // 콘서트, 스케줄, 좌석 생성
        val concert =
            concertRepository.save(
                Concert(
                    title = "Test Concert",
                    description = "Test Description",
                    concertStatus = ConcertStatus.AVAILABLE,
                ),
            )
        concertId = concert.id

        val schedule =
            concertScheduleRepository.save(
                ConcertSchedule(
                    concert = concert,
                    concertAt = LocalDateTime.now().plusDays(1),
                    reservationAvailableAt = LocalDateTime.now().minusHours(1),
                ),
            )
        scheduleId = schedule.id

        val seat =
            seatRepository.save(
                Seat(
                    concertSchedule = schedule,
                    seatStatus = SeatStatus.AVAILABLE,
                    seatNumber = 1,
                    seatPrice = 10000,
                ),
            )
        seatId = seat.id

        // 1000명의 사용자와 큐 생성
        repeat(1000) {
            val user = userRepository.save(User(name = "User $it"))
            userIds.add(user.id)
            val token = UUID.randomUUID().toString()
            tokens.add(token)
            queueRedisRepository.addToWaitingQueue(
                token,
                user.id.toString(),
                System.currentTimeMillis() + 1000 * 60 * 5, // 5분
            )
        }
    }

    @Test
    fun `1000명의 사용자가 동시에 한 좌석 예약 시 한 명만 성공해야 한다`() {
        val numberOfUsers = 1000
        val executorService = Executors.newFixedThreadPool(numberOfUsers)
        val latch = CountDownLatch(numberOfUsers)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)
        val startTime = System.nanoTime()
        repeat(numberOfUsers) { index ->
            executorService.submit {
                val userId = userIds[index]
                val token = tokens[index]

                try {
                    val reservationRequest =
                        ReservationServiceDto.Request(
                            userId = userId,
                            concertId = concertId,
                            scheduleId = scheduleId,
                            seatIds = listOf(seatId),
                        )
                    val result = reservationService.createReservations(token, reservationRequest)
                    if (result.isNotEmpty()) {
                        successCount.incrementAndGet()
                    } else {
                        failCount.incrementAndGet()
                    }
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await() // 모든 스레드가 작업을 마칠 때까지 대기

        val endTime = System.nanoTime()
        val duration = Duration.ofNanos(endTime - startTime)

        assertEquals(1, successCount.get(), "오직 한 명의 사용자만 예약에 성공해야 합니다")
        assertEquals(999, failCount.get(), "999명의 사용자는 예약에 실패해야 합니다")

        // 좌석 상태 확인
        val reservedSeat = seatRepository.findById(seatId)!!
        assertEquals(SeatStatus.UNAVAILABLE, reservedSeat.seatStatus, "예약된 좌석의 상태는 UNAVAILABLE이어야 합니다")
        println("테스트 실행 시간: ${duration.toMillis()} 밀리초")
    }
}
