package com.hhplus.concert.business.domain.message

interface MessageClient {
    fun sendMessage(alarm: MessageAlarmPayload): Any?
}
