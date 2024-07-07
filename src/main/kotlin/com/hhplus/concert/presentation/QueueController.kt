package com.hhplus.concert.presentation

import com.hhplus.concert.application.facade.QueueService
import com.hhplus.concert.common.annotation.TokenRequired
import com.hhplus.concert.common.annotation.ValidatedToken
import com.hhplus.concert.presentation.response.QueueTokenResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/queue")
class QueueController(
    private val queueService: QueueService,
) {
    /**
     * Queue 에 저장하고, token 을 발급한다.
     */
    @PostMapping("/users/{userId}")
    fun issueQueueToken(
        @PathVariable userId: Long,
    ): QueueTokenResponse.Token = QueueTokenResponse.Token.from(queueService.issueQueueToken(userId))

    /**
     * 토큰 정보와, userId 로 queue 의 상태를 확인한다.
     * 이 api 는 client 에서 주기적인 poling 을 통해 조회 한다고 가정한다.
     * userId 는 resolver 에서 header 로 받은 jwt 토큰으로 인증 받아 가져온다.
     */
    @TokenRequired
    @GetMapping("/users")
    fun getQueueStatus(
        @ValidatedToken token: String,
    ): QueueTokenResponse.Queue = QueueTokenResponse.Queue.from(queueService.findQueueByToken(token))
}
