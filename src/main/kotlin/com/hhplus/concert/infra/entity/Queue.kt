package com.hhplus.concert.infra.entity

import com.hhplus.concert.common.type.QueueStatus
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalDateTime

@Entity
class Queue(
    user: User,
    token: String,
    joinedAt: LocalDateTime,
    queueStatus: QueueStatus,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @ManyToOne
    @JoinColumn(name = "user_id", foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var user: User = user
        protected set

    @Column(name = "token", nullable = false)
    var token: String = token
        protected set

    @Column(name = "joined_at", nullable = false)
    var joinedAt: LocalDateTime = joinedAt
        protected set

    @Column(name = "status", nullable = false)
    var queueStatus: QueueStatus = queueStatus
        protected set

    fun updateStatus(queueStatus: QueueStatus) {
        this.queueStatus = queueStatus
    }
}
