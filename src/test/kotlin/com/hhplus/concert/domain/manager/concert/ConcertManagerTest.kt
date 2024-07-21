package com.hhplus.concert.domain.manager.concert

import com.hhplus.concert.business.domain.entity.Concert
import com.hhplus.concert.business.domain.entity.ConcertSchedule
import com.hhplus.concert.business.domain.entity.Seat
import com.hhplus.concert.business.domain.manager.ConcertManager
import com.hhplus.concert.business.domain.repository.ConcertRepository
import com.hhplus.concert.business.domain.repository.ConcertScheduleRepository
import com.hhplus.concert.business.domain.repository.SeatRepository
import com.hhplus.concert.common.error.exception.BusinessException
import com.hhplus.concert.common.type.ConcertStatus
import com.hhplus.concert.common.type.SeatStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.time.LocalDateTime

/**
 * ConcertManager 단위 테스트
 */
class ConcertManagerTest {
    @Mock
    private lateinit var concertRepository: ConcertRepository

    @Mock
    private lateinit var concertScheduleRepository: ConcertScheduleRepository

    @Mock
    private lateinit var seatRepository: SeatRepository

    @InjectMocks
    private lateinit var concertManager: ConcertManager

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `예약 가능한 콘서트만 반환해야 한다`() {
        `when`(concertRepository.findAll()).thenReturn(listOf(AVAILABLE_CONCERT, UNAVAILABLE_CONCERT))

        // when
        val result = concertManager.getAvailableConcerts()

        // then
        assertThat(result.size).isEqualTo(1)
        assertThat(result[0].title).isEqualTo("availableConcert")
        assertThat(result[0].description).isEqualTo("available")
        assertThat(result[0].concertStatus).isEqualTo(ConcertStatus.AVAILABLE)
    }

    @Test
    fun `예약 가능한 콘서트 스케줄만 반환해야 한다`() {
        // given
        val concertId = 0L
        `when`(concertRepository.findById(concertId)).thenReturn(AVAILABLE_CONCERT)
        `when`(concertScheduleRepository.findAllByConcertId(concertId)).thenReturn(listOf(VALID_SCHEDULE, INVALID_SCHEDULE))

        // when
        val result = concertManager.getAvailableConcertSchedules(concertId)

        // then
        assertThat(result.size).isEqualTo(1)
        assertThat(result[0]).isEqualTo(VALID_SCHEDULE)
    }

    @Test
    fun `예약 불가능한 콘서트에 대해 예외를 발생시켜야 한다`() {
        // given
        val concertId = 0L

        `when`(concertRepository.findById(concertId)).thenReturn(UNAVAILABLE_CONCERT)

        // when & then
        assertThrows<BusinessException.BadRequest> {
            concertManager.getAvailableConcertSchedules(concertId)
        }
    }

    @Test
    fun `유효한 콘서트와 스케줄에 대해 좌석을 반환해야 한다`() {
        // given
        val concertId = 0L
        val scheduleId = 0L

        val seats =
            listOf(
                SEAT1,
                SEAT2,
            )

        `when`(concertRepository.findById(concertId)).thenReturn(AVAILABLE_CONCERT)
        `when`(concertScheduleRepository.findById(scheduleId)).thenReturn(VALID_SCHEDULE)
        `when`(seatRepository.findAllByScheduleId(scheduleId)).thenReturn(seats)

        // when
        val result = concertManager.getAvailableSeats(concertId, scheduleId)

        // then
        assertThat(result).isEqualTo(seats)
    }

    @Test
    fun `예약 불가능한 콘서트에 대해 좌석 조회 시 예외를 발생시켜야 한다`() {
        val concertId = 0L
        val scheduleId = 0L

        `when`(concertRepository.findById(concertId)).thenReturn(UNAVAILABLE_CONCERT)

        assertThrows<BusinessException.BadRequest> {
            concertManager.getAvailableSeats(concertId, scheduleId)
        }
    }

    @Test
    fun `유효하지 않은 스케줄에 대해 좌석 조회 시 예외를 발생시켜야 한다`() {
        val concertId = 1L
        val scheduleId = 1L

        `when`(concertRepository.findById(concertId)).thenReturn(AVAILABLE_CONCERT)
        `when`(concertScheduleRepository.findById(scheduleId)).thenReturn(INVALID_SCHEDULE)

        assertThrows<BusinessException.BadRequest> {
            concertManager.getAvailableSeats(concertId, scheduleId)
        }
    }

    companion object {
        val AVAILABLE_CONCERT =
            Concert(
                title = "availableConcert",
                description = "available",
                concertStatus = ConcertStatus.AVAILABLE,
            )
        val UNAVAILABLE_CONCERT =
            Concert(
                title = "unavailableConcert",
                description = "unavailable",
                concertStatus = ConcertStatus.UNAVAILABLE,
            )

        val VALID_SCHEDULE =
            ConcertSchedule(
                AVAILABLE_CONCERT,
                concertAt = LocalDateTime.now().plusDays(2),
                reservationAvailableAt = LocalDateTime.now().minusDays(1),
            )
        val INVALID_SCHEDULE =
            ConcertSchedule(
                AVAILABLE_CONCERT,
                concertAt = LocalDateTime.now().minusDays(1),
                reservationAvailableAt = LocalDateTime.now().minusDays(2),
            )
        val SEAT1 =
            Seat(
                concertSchedule = VALID_SCHEDULE,
                seatNumber = 1,
                seatStatus = SeatStatus.AVAILABLE,
                seatPrice = 10000,
            )
        val SEAT2 =
            Seat(
                concertSchedule = VALID_SCHEDULE,
                seatNumber = 2,
                seatStatus = SeatStatus.AVAILABLE,
                seatPrice = 10000,
            )
    }
}
