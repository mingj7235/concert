package com.hhplus.concert.application.facade.integration

import com.hhplus.concert.business.application.service.PaymentService
import com.hhplus.concert.business.domain.entity.Concert
import com.hhplus.concert.business.domain.entity.ConcertSchedule
import com.hhplus.concert.business.domain.entity.Queue
import com.hhplus.concert.business.domain.entity.Reservation
import com.hhplus.concert.business.domain.entity.Seat
import com.hhplus.concert.business.domain.entity.User
import com.hhplus.concert.business.domain.repository.ConcertRepository
import com.hhplus.concert.business.domain.repository.ConcertScheduleRepository
import com.hhplus.concert.business.domain.repository.PaymentHistoryRepository
import com.hhplus.concert.business.domain.repository.PaymentRepository
import com.hhplus.concert.business.domain.repository.QueueRepository
import com.hhplus.concert.business.domain.repository.ReservationRepository
import com.hhplus.concert.business.domain.repository.SeatRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.error.exception.BusinessException
import com.hhplus.concert.common.type.ConcertStatus
import com.hhplus.concert.common.type.PaymentStatus
import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.common.type.ReservationStatus
import com.hhplus.concert.common.type.SeatStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@Transactional
class PaymentServiceIntegrationTest {
    @Autowired
    private lateinit var paymentService: PaymentService

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

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var paymentHistoryRepository: PaymentHistoryRepository

    @Test
    fun `유효한 요청으로 결제를 성공적으로 처리해야 한다`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        val token = "test_token"
        val queue =
            queueRepository.save(
                Queue(
                    user = user,
                    token = token,
                    joinedAt = LocalDateTime.now(),
                    queueStatus = QueueStatus.PROCESSING,
                ),
            )

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
        val seat =
            seatRepository.save(
                Seat(
                    concertSchedule = schedule,
                    seatStatus = SeatStatus.UNAVAILABLE,
                    seatNumber = 1,
                    seatPrice = 10000,
                ),
            )

        val reservation =
            reservationRepository.save(
                Reservation(
                    user = user,
                    concertTitle = concert.title,
                    concertAt = schedule.concertAt,
                    seat = seat,
                    reservationStatus = ReservationStatus.PAYMENT_PENDING,
                    createdAt = LocalDateTime.now(),
                ),
            )

        // when
        val result = paymentService.executePayment(token, user.id, listOf(reservation.id))

        // then
        assertEquals(1, result.size)
        assertEquals(10000, result[0].amount)
        assertEquals(PaymentStatus.COMPLETED, result[0].paymentStatus)

        val updatedReservation = reservationRepository.findById(reservation.id)
        assertEquals(ReservationStatus.PAYMENT_COMPLETED, updatedReservation!!.reservationStatus)

        val updatedQueue = queueRepository.findById(queue!!.id)
        assertEquals(QueueStatus.COMPLETED, updatedQueue!!.queueStatus)
    }

    @Test
    fun `요청한 사용자와 예약의 사용자가 일치하지 않을 경우 예외를 발생시켜야 한다`() {
        // given
        val user1 = userRepository.save(User(name = "User 1"))
        val user2 = userRepository.save(User(name = "User 2"))
        val token = "test_token"
        queueRepository.save(
            Queue(
                user = user1,
                token = token,
                joinedAt = LocalDateTime.now(),
                queueStatus = QueueStatus.PROCESSING,
            ),
        )

        val reservation =
            reservationRepository.save(
                Reservation(
                    user = user2, // 다른 사용자의 예약
                    concertTitle = "Test Concert",
                    concertAt = LocalDateTime.now().plusDays(1),
                    seat =
                        seatRepository.save(
                            Seat(
                                concertSchedule =
                                    concertScheduleRepository.save(
                                        ConcertSchedule(
                                            concert =
                                                concertRepository.save(
                                                    Concert(
                                                        title = "Test Concert",
                                                        description = "Test Description",
                                                        concertStatus = ConcertStatus.AVAILABLE,
                                                    ),
                                                ),
                                            concertAt = LocalDateTime.now().plusDays(1),
                                            reservationAvailableAt = LocalDateTime.now().minusHours(1),
                                        ),
                                    ),
                                seatStatus = SeatStatus.UNAVAILABLE,
                                seatNumber = 1,
                                seatPrice = 10000,
                            ),
                        ),
                    reservationStatus = ReservationStatus.PAYMENT_PENDING,
                    createdAt = LocalDateTime.now(),
                ),
            )

        // when & then
        assertThrows<BusinessException.BadRequest> {
            paymentService.executePayment(token, user1.id, listOf(reservation.id))
        }
    }

    @Test
    fun `존재하지 않는 예약 ID로 요청시 예외를 발생시켜야 한다`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        val token = "test_token"
        queueRepository.save(
            Queue(
                user = user,
                token = token,
                joinedAt = LocalDateTime.now(),
                queueStatus = QueueStatus.PROCESSING,
            ),
        )

        val nonExistentReservationId = 99999L

        // when & then
        assertThrows<BusinessException.BadRequest> {
            paymentService.executePayment(token, user.id, listOf(nonExistentReservationId))
        }
    }

    @Test
    fun `여러 예약에 대한 결제를 한 번에 처리할 수 있어야 한다`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        val token = "test_token"
        val queue =
            queueRepository.save(
                Queue(
                    user = user,
                    token = token,
                    joinedAt = LocalDateTime.now(),
                    queueStatus = QueueStatus.PROCESSING,
                ),
            )

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

        val seat1 =
            seatRepository.save(
                Seat(
                    concertSchedule = schedule,
                    seatStatus = SeatStatus.UNAVAILABLE,
                    seatNumber = 1,
                    seatPrice = 10000,
                ),
            )
        val seat2 =
            seatRepository.save(
                Seat(
                    concertSchedule = schedule,
                    seatStatus = SeatStatus.UNAVAILABLE,
                    seatNumber = 2,
                    seatPrice = 20000,
                ),
            )

        val reservation1 =
            reservationRepository.save(
                Reservation(
                    user = user,
                    concertTitle = concert.title,
                    concertAt = schedule.concertAt,
                    seat = seat1,
                    reservationStatus = ReservationStatus.PAYMENT_PENDING,
                    createdAt = LocalDateTime.now(),
                ),
            )
        val reservation2 =
            reservationRepository.save(
                Reservation(
                    user = user,
                    concertTitle = concert.title,
                    concertAt = schedule.concertAt,
                    seat = seat2,
                    reservationStatus = ReservationStatus.PAYMENT_PENDING,
                    createdAt = LocalDateTime.now(),
                ),
            )

        // when
        val result = paymentService.executePayment(token, user.id, listOf(reservation1.id, reservation2.id))

        // then
        assertEquals(2, result.size)
        assertEquals(10000, result[0].amount)
        assertEquals(20000, result[1].amount)
        result.forEach { assertEquals(PaymentStatus.COMPLETED, it.paymentStatus) }

        val updatedReservations = reservationRepository.findAllById(listOf(reservation1.id, reservation2.id))
        updatedReservations.forEach { assertEquals(ReservationStatus.PAYMENT_COMPLETED, it.reservationStatus) }

        val updatedQueue = queueRepository.findById(queue!!.id)
        assertEquals(QueueStatus.COMPLETED, updatedQueue!!.queueStatus)
    }
}
