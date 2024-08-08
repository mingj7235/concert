package com.hhplus.concert.infrastructure.client

import com.hhplus.concert.business.domain.message.MessageAlarmPayload
import com.hhplus.concert.business.domain.message.MessageClient
import com.hhplus.concert.common.type.AlarmLevel
import com.slack.api.Slack
import com.slack.api.model.Attachments.asAttachments
import com.slack.api.model.Attachments.attachment
import com.slack.api.model.kotlin_extension.block.withBlocks
import com.slack.api.webhook.Payload
import com.slack.api.webhook.WebhookPayloads
import com.slack.api.webhook.WebhookResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SlackClient(
    @Value("\${slack.checkin.webhook.base_url}") private val baseUrl: String,
    @Value("\${spring.profiles.active}") private val profile: String,
) : MessageClient {
    override fun sendMessage(alarm: MessageAlarmPayload): WebhookResponse? = Slack.getInstance().send(baseUrl, getSlackPayload(alarm))

    fun getSlackPayload(alarm: MessageAlarmPayload): Payload {
        val blocks =
            withBlocks {
                context {
                    elements {
                        markdownText(text = "*Profile*")
                        plainText(text = profile)
                    }
                    elements {
                        markdownText(text = "*Alarm Level*")
                        plainText(text = alarm.alarmLevel.name)
                    }
                    elements {
                        markdownText(text = "*Description*")
                        plainText(text = alarm.description)
                    }
                }
            }
        val color =
            when (alarm.alarmLevel) {
                AlarmLevel.PRIMARY -> Color.PRIMARY
                AlarmLevel.INFO -> Color.INFO
                AlarmLevel.SUCCESS -> Color.SUCCESS
                AlarmLevel.WARNING -> Color.WARNING
                AlarmLevel.DANGER -> Color.DANGER
            }

        return WebhookPayloads.payload { payload ->
            payload
                .text(alarm.subject)
                .attachments(
                    asAttachments(
                        attachment {
                            it
                                .color(color.code)
                                .blocks(blocks)
                        },
                    ),
                )
        }
    }

    companion object {
        enum class Color(
            val code: String,
        ) {
            PRIMARY(code = "#007bff"),
            INFO(code = "#17a2b8"),
            SUCCESS(code = "#28a745"),
            WARNING(code = "#ffc107"),
            DANGER(code = "#dc3545"),
        }
    }
}
