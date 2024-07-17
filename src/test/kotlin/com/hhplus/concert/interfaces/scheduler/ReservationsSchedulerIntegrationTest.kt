package com.hhplus.concert.interfaces.scheduler

import com.hhplus.concert.business.domain.repository.ConcertRepository
import com.hhplus.concert.business.domain.repository.ConcertScheduleRepository
import com.hhplus.concert.business.domain.repository.ReservationRepository
import com.hhplus.concert.business.domain.repository.SeatRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.type.ConcertStatus
import com.hhplus.concert.common.type.ReservationStatus
import com.hhplus.concert.common.type.SeatStatus
import com.hhplus.concert.infrastructure.entity.Concert
import com.hhplus.concert.infrastructure.entity.ConcertSchedule
import com.hhplus.concert.infrastructure.entity.Reservation
import com.hhplus.concert.infrastructure.entity.Seat
import com.hhplus.concert.infrastructure.entity.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationsSchedulerIntegrationTest {
    @Autowired
    private lateinit var reservationScheduler: ReservationScheduler

    @Autowired
    private lateinit var reservationRepository: ReservationRepository

    @Autowired
    private lateinit var seatRepository: SeatRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var concertRepository: ConcertRepository

    @Autowired
    private lateinit var scheduleRepository: ConcertScheduleRepository

    @Test
    fun `스케줄러가 만료된 예약을 취소하고 좌석을 해제해야 한다`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        val concert =
            concertRepository.save(
                Concert(title = "Test Concert", description = "Test Description", concertStatus = ConcertStatus.AVAILABLE),
            )
        val schedule =
            scheduleRepository.save(
                ConcertSchedule(
                    concert = concert,
                    concertAt = LocalDateTime.now().plusDays(1),
                    reservationAvailableAt = LocalDateTime.now().minusHours(1),
                ),
            )

        val seat1 =
            seatRepository.save(
                Seat(concertSchedule = schedule, seatStatus = SeatStatus.UNAVAILABLE, seatNumber = 1, seatPrice = 10000),
            )
        val seat2 =
            seatRepository.save(
                Seat(concertSchedule = schedule, seatStatus = SeatStatus.UNAVAILABLE, seatNumber = 2, seatPrice = 10000),
            )
        val seat3 =
            seatRepository.save(
                Seat(concertSchedule = schedule, seatStatus = SeatStatus.UNAVAILABLE, seatNumber = 3, seatPrice = 10000),
            )

        val expiredReservation1 =
            reservationRepository.save(
                Reservation(
                    user = user,
                    concertTitle = concert.title,
                    concertAt = schedule.concertAt,
                    seat = seat1,
                    reservationStatus = ReservationStatus.PAYMENT_PENDING,
                    createdAt = LocalDateTime.now().minusMinutes(6),
                ),
            )
        val expiredReservation2 =
            reservationRepository.save(
                Reservation(
                    user = user,
                    concertTitle = concert.title,
                    concertAt = schedule.concertAt,
                    seat = seat2,
                    reservationStatus = ReservationStatus.PAYMENT_PENDING,
                    createdAt = LocalDateTime.now().minusMinutes(6),
                ),
            )
        val activeReservation =
            reservationRepository.save(
                Reservation(
                    user = user,
                    concertTitle = concert.title,
                    concertAt = schedule.concertAt,
                    seat = seat3,
                    reservationStatus = ReservationStatus.PAYMENT_PENDING,
                    createdAt = LocalDateTime.now(),
                ),
            )

        // when
        reservationScheduler.cancelExpiredReservations()

        val updatedExpiredReservation1 = reservationRepository.findById(expiredReservation1.id)
        val updatedExpiredReservation2 = reservationRepository.findById(expiredReservation2.id)
        val updatedActiveReservation = reservationRepository.findById(activeReservation.id)

        assertEquals(ReservationStatus.RESERVATION_CANCELLED, updatedExpiredReservation1!!.reservationStatus)
        assertEquals(ReservationStatus.RESERVATION_CANCELLED, updatedExpiredReservation2!!.reservationStatus)
        assertEquals(ReservationStatus.PAYMENT_PENDING, updatedActiveReservation!!.reservationStatus)

        val updatedSeat1 = seatRepository.findById(seat1.id)
        val updatedSeat2 = seatRepository.findById(seat2.id)
        val updatedSeat3 = seatRepository.findById(seat3.id)

        assertEquals(SeatStatus.AVAILABLE, updatedSeat1!!.seatStatus)
        assertEquals(SeatStatus.AVAILABLE, updatedSeat2!!.seatStatus)
        assertEquals(SeatStatus.UNAVAILABLE, updatedSeat3!!.seatStatus)
    }

    @Test
    fun `스케줄러가 만료된 예약이 없을 때 아무 작업도 수행하지 않아야 한다`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        val concert =
            concertRepository.save(
                Concert(title = "Test Concert", description = "Test Description", concertStatus = ConcertStatus.AVAILABLE),
            )
        val schedule =
            scheduleRepository.save(
                ConcertSchedule(
                    concert = concert,
                    concertAt = LocalDateTime.now().plusDays(1),
                    reservationAvailableAt = LocalDateTime.now().minusHours(1),
                ),
            )

        val seat =
            seatRepository.save(
                Seat(concertSchedule = schedule, seatStatus = SeatStatus.UNAVAILABLE, seatNumber = 1, seatPrice = 10000),
            )
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
        reservationScheduler.cancelExpiredReservations()

        // then
        val allReservations = reservationRepository.findAll()
        assertEquals(1, allReservations.size)
        assertEquals(ReservationStatus.PAYMENT_PENDING, allReservations[0].reservationStatus)

        val allSeats = seatRepository.findAllById(listOf(seat.id))
        assertEquals(1, allSeats.size)
        assertEquals(SeatStatus.UNAVAILABLE, allSeats[0].seatStatus)
    }
}
