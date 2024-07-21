package com.hhplus.concert.business.domain.entity

import com.hhplus.concert.common.type.PaymentStatus
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalDateTime

@Entity
class Payment(
    user: User,
    reservation: Reservation,
    amount: Int,
    executedAt: LocalDateTime,
    paymentStatus: PaymentStatus,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @ManyToOne
    @JoinColumn(name = "user_id", foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var user: User = user
        protected set

    @ManyToOne
    @JoinColumn(name = "reservation_id", foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var reservation: Reservation = reservation
        protected set

    @Column(name = "amount", nullable = false)
    var amount: Int = amount
        protected set

    @Column(name = "executed_at", nullable = false)
    var executedAt: LocalDateTime = executedAt
        protected set

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var paymentStatus: PaymentStatus = paymentStatus
        protected set
}
