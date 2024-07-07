package com.hhplus.concert.infra.entity

import com.hhplus.concert.common.type.PaymentStatus
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
class PaymentHistory(
    user: User,
    amount: Long,
    createdAt: LocalDateTime,
    paymentStatus: PaymentStatus,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @ManyToOne
    @JoinColumn(name = "user_id", foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var user: User = user
        protected set

    @Column(name = "amount", nullable = false)
    var amount: Long = amount
        protected set

    @Column(name = "create_at", nullable = false)
    var createdAt: LocalDateTime = createdAt
        protected set

    @Column(name = "status", nullable = false)
    var paymentStatus: PaymentStatus = paymentStatus
        protected set
}
