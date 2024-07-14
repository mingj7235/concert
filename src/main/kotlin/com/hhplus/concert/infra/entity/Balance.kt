package com.hhplus.concert.infra.entity

import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import java.time.LocalDateTime

@Entity
class Balance(
    user: User,
    amount: Long,
    lastUpdatedAt: LocalDateTime,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @OneToOne
    @JoinColumn(name = "user_id", foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var user: User = user
        protected set

    @Column(name = "amount", nullable = false)
    var amount: Long = amount
        protected set

    @Column(name = "last_update_at", nullable = false)
    var lastUpdatedAt: LocalDateTime = lastUpdatedAt
        protected set

    fun updateAmount(amount: Long) {
        this.amount += amount
        this.lastUpdatedAt = LocalDateTime.now()
    }
}
