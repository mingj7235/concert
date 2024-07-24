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
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis
import kotlin.test.assertTrue

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
    private lateinit var seatRepository: SeatRepository

    @Autowired
    private lateinit var reservationRepository: ReservationRepository

    private lateinit var testUser: User
    private lateinit var testConcert: Concert
    private lateinit var testSchedule: ConcertSchedule
    private lateinit var testSeats: List<Seat>
    private lateinit var testSeat: Seat

    @BeforeEach
    fun setup() {
        testUser = userRepository.save(User(name = "Test User"))
        testConcert =
            concertRepository.save(
                Concert(
                    title = "Test Concert",
                    description = "Test Description",
                    concertStatus = ConcertStatus.AVAILABLE,
                ),
            )
        testSchedule =
            concertScheduleRepository.save(
                ConcertSchedule(
                    concert = testConcert,
                    concertAt = LocalDateTime.now().plusDays(1),
                    reservationAvailableAt = LocalDateTime.now().minusHours(1),
                ),
            )

        queueRepository.save(
            Queue(user = testUser, token = "test_token", joinedAt = LocalDateTime.now(), queueStatus = QueueStatus.PROCESSING),
        )
    }

    @Test
    fun `SpinLock을 사용할 때 여러 요청이 동시에 들어와도 모든 요청이 eventually 처리된다`() {
        val numberOfThreads = 10
        val executor = Executors.newFixedThreadPool(numberOfThreads)
        val successfulReservations = AtomicInteger(0)
        val failedReservations = AtomicInteger(0)

        testSeats =
            (1..10).map {
                seatRepository.save(
                    Seat(concertSchedule = testSchedule, seatStatus = SeatStatus.AVAILABLE, seatNumber = it, seatPrice = 10000),
                )
            }

        val totalTime =
            measureTimeMillis {
                val futures =
                    (1..numberOfThreads).map {
                        executor.submit {
                            try {
                                val request =
                                    ReservationServiceDto.Request(
                                        userId = testUser.id,
                                        concertId = testConcert.id,
                                        scheduleId = testSchedule.id,
                                        seatIds = listOf(testSeats[it - 1].id),
                                    )
                                reservationService.createReservations("test_token", request)
                                successfulReservations.incrementAndGet()
                            } catch (e: Exception) {
                                failedReservations.incrementAndGet()
                            }
                        }
                    }

                futures.forEach { it.get() }
            }

        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.MINUTES)

        assertEquals(numberOfThreads, successfulReservations.get(), "모든 예약 요청이 성공해야 합니다")
        assertEquals(0, failedReservations.get(), "실패한 예약이 없어야 합니다")

        val reservedSeats = seatRepository.findAllById(testSeats.map { it.id })
        assertTrue(reservedSeats.all { it.seatStatus == SeatStatus.UNAVAILABLE }, "모든 좌석이 예약되어야 합니다")

        println("총 실행 시간: $totalTime ms")
        println("평균 예약 시간: ${totalTime.toDouble() / numberOfThreads} ms")
    }

    @Test
    fun `SpinLock을 사용할 때 여러 요청이 같은 좌석을 예약하려고 하면 하나만 성공하고 나머지는 실패한다`() {
        val numberOfThreads = 10
        val executor = Executors.newFixedThreadPool(numberOfThreads)
        val successfulReservations = AtomicInteger(0)
        val failedReservations = AtomicInteger(0)

        testSeat =
            seatRepository.save(
                Seat(
                    concertSchedule = testSchedule,
                    seatStatus = SeatStatus.AVAILABLE,
                    seatNumber = 1,
                    seatPrice = 10000,
                ),
            )

        val totalTime =
            measureTimeMillis {
                val futures =
                    (1..numberOfThreads).map {
                        executor.submit {
                            try {
                                val request =
                                    ReservationServiceDto.Request(
                                        userId = testUser.id,
                                        concertId = testConcert.id,
                                        scheduleId = testSchedule.id,
                                        seatIds = listOf(testSeat.id),
                                    )
                                reservationService.createReservations("test_token", request)
                                successfulReservations.incrementAndGet()
                            } catch (e: Exception) {
                                failedReservations.incrementAndGet()
                            }
                        }
                    }

                futures.forEach { it.get() }
            }

        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.MINUTES)

        assertEquals(1, successfulReservations.get(), "하나의 예약만 성공해야 합니다")
        assertEquals(numberOfThreads - 1, failedReservations.get(), "나머지 예약은 실패해야 합니다")

        val reservedSeat = seatRepository.findById(testSeat.id)!!
        assertEquals(SeatStatus.UNAVAILABLE, reservedSeat.seatStatus, "좌석이 예약되어야 합니다")

        val reservations = reservationRepository.findAll()
        assertEquals(1, reservations.size, "하나의 예약만 생성되어야 합니다")

        println("총 실행 시간: $totalTime ms")
        println("평균 시도 시간: ${totalTime.toDouble() / numberOfThreads} ms")
    }
}
