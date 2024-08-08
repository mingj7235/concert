package com.hhplus.concert.business.domain.message

import com.hhplus.concert.common.type.AlarmLevel

data class MessageAlarmPayload(
    val alarmLevel: AlarmLevel,
    val subject: String,
    val description: String,
)
