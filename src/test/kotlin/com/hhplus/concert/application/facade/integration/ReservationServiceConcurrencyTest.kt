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
import com.hhplus.concert.business.domain.repository.SeatRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.error.exception.BusinessException
import com.hhplus.concert.common.type.ConcertStatus
import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.common.type.SeatStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

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

    @Test
    fun `동시에 같은 좌석 10회 예약 시 한 요청만 성공해야 한다`() {
        // Given
        val users =
            (1..10).map {
                userRepository.save(User(name = "Test User $it"))
            }

        val concert =
            concertRepository.save(
                Concert(
                    title = "Test Concert",
                    description = "Test Description",
                    concertStatus = ConcertStatus.AVAILABLE,
                ),
            )
        val schedule =
            concertScheduleRepository.save(
                ConcertSchedule(
                    concert = concert,
                    concertAt = LocalDateTime.now().plusDays(1),
                    reservationAvailableAt = LocalDateTime.now().minusHours(1),
                ),
            )
        val seat = seatRepository.save(Seat(schedule, 1, SeatStatus.AVAILABLE, 10000))

        val threadCount = 10
        val executorService = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        // When
        val results = Collections.synchronizedList(mutableListOf<Result<List<ReservationServiceDto.Result>>>())
        repeat(threadCount) { index ->
            executorService.submit {
                try {
                    val user = users[index]
                    val token = "test_token_${user.id}"
                    queueRepository.save(
                        Queue(
                            user = user,
                            token = token,
                            joinedAt = LocalDateTime.now(),
                            queueStatus = QueueStatus.PROCESSING,
                        ),
                    )
                    val reservationRequest =
                        ReservationServiceDto.Request(
                            userId = user.id,
                            concertId = 1L,
                            scheduleId = 1L,
                            seatIds = listOf(seat.id),
                        )

                    val result =
                        runCatching {
                            reservationService.createReservations(token, reservationRequest)
                        }
                    results.add(result)
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()

        // Then
        val successCount = results.count { it.isSuccess }
        val failureCount = results.count { it.isFailure }

        assert(successCount == 1) { "성공한 예약이 1개여야 합니다." }
        assert(failureCount == 9) { "실패한 예약이 9개여야 합니다." }
        assert(results.count { it.isFailure && it.exceptionOrNull() is BusinessException.BadRequest } == 9) {
            "실패한 9개의 예약은 모두 BusinessException.BadRequest 예외를 발생시켜야 합니다."
        }

        val updatedSeat = seatRepository.findById(seat.id)!!
        assert(updatedSeat.seatStatus == SeatStatus.UNAVAILABLE) { "좌석 상태가 UNAVAILABLE로 변경되어야 합니다." }
    }
}
