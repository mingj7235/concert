package com.hhplus.concert.infrastructure.client

data class SlackAlarmPayload(
    val alarmLevel: AlarmLevel,
    val subject: String,
    val description: String,
)
