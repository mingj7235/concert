package com.hhplus.concert.interfaces.presentation.controller

import com.hhplus.concert.business.application.service.ConcertService
import com.hhplus.concert.common.annotation.TokenRequired
import com.hhplus.concert.common.annotation.ValidatedToken
import com.hhplus.concert.interfaces.presentation.response.ConcertResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class ConcertController(
    private val concertService: ConcertService,
) {
    /**
     * 현재 예약 가능한 콘서트의 목록을 조회한다.
     */
    @TokenRequired
    @GetMapping("/concerts")
    fun getAvailableConcerts(
        @ValidatedToken token: String,
    ): List<ConcertResponse.Concert> =
        concertService
            .getAvailableConcerts(token)
            .map {
                ConcertResponse.Concert.from(it)
            }

    /**
     * 콘서트 예약 가능 날짜 목록을 조회한다.
     */
    @TokenRequired
    @GetMapping("/concerts/{concertId}/schedules")
    fun getConcertSchedules(
        @ValidatedToken token: String,
        @PathVariable concertId: Long,
    ): ConcertResponse.Schedule =
        ConcertResponse.Schedule.from(
            concertService.getConcertSchedules(
                token = token,
                concertId = concertId,
            ),
        )

    /**
     * 콘서트 해당 날짜의 좌석을 조회한다.
     */
    @TokenRequired
    @GetMapping("/concerts/{concertId}/schedules/{scheduleId}/seats")
    fun getAvailableSeats(
        @ValidatedToken token: String,
        @PathVariable concertId: Long,
        @PathVariable scheduleId: Long,
    ): ConcertResponse.AvailableSeat =
        ConcertResponse.AvailableSeat.from(
            concertService.getAvailableSeats(
                token = token,
                concertId = concertId,
                scheduleId = scheduleId,
            ),
        )
}
