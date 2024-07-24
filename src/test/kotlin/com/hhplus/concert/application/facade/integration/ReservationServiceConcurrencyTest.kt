package com.hhplus.concert.application.facade.integration

import com.hhplus.concert.business.application.dto.ReservationServiceDto
import com.hhplus.concert.business.application.service.ReservationService
import com.hhplus.concert.business.domain.entity.Concert
import com.hhplus.concert.business.domain.entity.ConcertSchedule
import com.hhplus.concert.business.domain.entity.Queue
import com.hhplus.concert.business.domain.entity.Seat
import com.hhplus.concert.business.domain.entity.User
import com.hhplus.concert.business.domain.repository.ConcertRepository
import com.hhplus.concert.business.domain.repository.ConcertScheduleRepository
import com.hhplus.concert.business.domain.repository.QueueRepository
import com.hhplus.concert.business.domain.repository.ReservationRepository
import com.hhplus.concert.business.domain.repository.SeatRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.type.ConcertStatus
import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.common.type.SeatStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class ReservationServiceConcurrencyTest {
    @Autowired
    private lateinit var reservationService: ReservationService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var queueRepository: QueueRepository

    @Autowired
    private lateinit var concertRepository: ConcertRepository

    @Autowired
    private lateinit var concertScheduleRepository: ConcertScheduleRepository

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
            queueRepository.save(
                Queue(
                    user = user,
                    token = token,
                    joinedAt = LocalDateTime.now(),
                    queueStatus = QueueStatus.PROCESSING,
                ),
            )
        }
    }

    @Test
    fun `1000명이 동시에 여러 예약 요청이 들어올 때 중복 예약이 발생하지 않아야 한다`() {
        val startTime = System.nanoTime()
        val numberOfThreads = 1000
        val successfulReservations = AtomicInteger(0)
        val failedReservations = AtomicInteger(0)
        val exceptionTypes = ConcurrentHashMap<String, AtomicInteger>()
        val executor = Executors.newFixedThreadPool(numberOfThreads)

        try {
            val futures =
                (0 until numberOfThreads).map { index ->
                    executor.submit {
                        val userId = userIds[index]
                        val token = tokens[index]

                        try {
                            val request =
                                ReservationServiceDto.Request(
                                    userId = userId,
                                    concertId = concertId,
                                    scheduleId = scheduleId,
                                    seatIds = listOf(seatId),
                                )
                            val result = reservationService.createReservations(token, request)
                            if (result.isNotEmpty()) {
                                successfulReservations.incrementAndGet()
                            }
                        } catch (e: Exception) {
                            failedReservations.incrementAndGet()
                            val exceptionName = e.javaClass.simpleName
                            exceptionTypes.computeIfAbsent(exceptionName) { AtomicInteger() }.incrementAndGet()
                        }
                    }
                }

            futures.forEach { it.get() }
        } finally {
            executor.shutdown()
            executor.awaitTermination(1, TimeUnit.MINUTES)
        }

        val endTime = System.nanoTime()
        val duration = Duration.ofNanos(endTime - startTime)

        println("테스트 실행 시간: ${duration.toMillis()} 밀리초")
        println("테스트 실행 시간: ${duration.toMillis()} 밀리초")
        println("성공한 예약 수: ${successfulReservations.get()}")
        println("실패한 예약 수: ${failedReservations.get()}")
        println("예외 유형별 발생 횟수:")
        exceptionTypes.forEach { (exceptionName, count) ->
            println("  $exceptionName: ${count.get()}회")
        }

        // 검증
        assertEquals(1, successfulReservations.get(), "오직 한 명만 예약에 성공해야 합니다")
        assertEquals(numberOfThreads - 1, failedReservations.get(), "나머지는 모두 실패해야 합니다")

        // 좌석 상태 확인
        val updatedSeat = seatRepository.findById(seatId)!!
        assertEquals(SeatStatus.UNAVAILABLE, updatedSeat.seatStatus, "예약된 좌석의 상태가 UNAVAILABLE이어야 합니다")

        // 예약 확인
        val reservations = reservationRepository.findAll()
        assertEquals(1, reservations.size, "오직 하나의 예약만 생성되어야 합니다")
    }
}
