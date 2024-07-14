package com.hhplus.concert.application.facade

import com.hhplus.concert.application.dto.ConcertServiceDto
import com.hhplus.concert.common.exception.error.QueueException
import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.domain.manager.concert.ConcertManager
import com.hhplus.concert.domain.manager.queue.QueueManager
import org.springframework.stereotype.Service

@Service
class ConcertService(
    private val concertManager: ConcertManager,
    private val queueManager: QueueManager,
) {
    /**
     * 1. token 을 통해 queue 상태를 검증한다. (processing 상태여야한다.)
     * 2. 현재 예약이 가능한 전체 concert 리스트를 dto 로 변환하여 리턴한다.
     */
    fun getAvailableConcerts(token: String): List<ConcertServiceDto.Concert> {
        validateQueueStatus(token)
        return concertManager
            .getAvailableConcerts()
            .map {
                ConcertServiceDto.Concert(
                    concertId = it.id,
                    title = it.title,
                    description = it.description,
                )
            }
    }

    /**
     * 1. token 을 통해 queue 상태를 검증한다. (processing 상태여야한다.)
     * 2. 현재 예약 가능한 concert 인지 확인하고, 해당 concertSchedule 을 조회한다.
     * 3. dto 형태로 변환하여 리턴한다.
     */
    fun getConcertSchedules(
        token: String,
        concertId: Long,
    ): ConcertServiceDto.Schedule {
        validateQueueStatus(token)
        return ConcertServiceDto.Schedule(
            concertId = concertId,
            events =
                concertManager
                    .getAvailableConcertSchedules(concertId)
                    .map {
                        ConcertServiceDto.Event(
                            scheduleId = it.id,
                            concertAt = it.concertAt,
                            reservationAt = it.reservationAvailableAt,
                        )
                    },
        )
    }

    /**
     * 1. token 을 통해 queue 상태를 검증한다. (processing 상태여야한다.)
     * 2. 현재 예약 가능한 concert 인지 확인하고, 해당 스케쥴의 예약 가능한 좌석을 조회한다.
     * 3. dto 형태로 변환하여 리턴한다.
     */
    fun getAvailableSeats(
        token: String,
        concertId: Long,
        scheduleId: Long,
    ): ConcertServiceDto.AvailableSeat {
        validateQueueStatus(token)
        return ConcertServiceDto.AvailableSeat(
            concertId = concertId,
            seats =
                concertManager
                    .getAvailableSeats(
                        concertId = concertId,
                        scheduleId = scheduleId,
                    ).map {
                        ConcertServiceDto.Seat(
                            seatId = it.id,
                            seatStatus = it.seatStatus,
                            seatNumber = it.seatNumber,
                            seatPrice = it.seatPrice,
                        )
                    },
        )
    }

    private fun validateQueueStatus(token: String) {
        val queue = queueManager.findByToken(token)
        if (queue.queueStatus != QueueStatus.PROCESSING) throw QueueException.NotAllowed()
    }
}
