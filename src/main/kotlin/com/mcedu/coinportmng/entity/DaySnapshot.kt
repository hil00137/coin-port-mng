package com.mcedu.coinportmng.entity

import jakarta.persistence.*
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@SequenceGenerator(
    name = Sequence.DAY_SNAPSHOT + "_gen",
    sequenceName = Sequence.DAY_SNAPSHOT,
    initialValue = 1,
    allocationSize = 1
)
@DynamicInsert
@DynamicUpdate
@EntityListeners(AuditingEntityListener::class)
@Table(name = "day_snapshot", indexes = [Index(name = "idx_day_snapshot", columnList = "access_info_seq, time")])
data class DaySnapshot(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = Sequence.DAY_SNAPSHOT + "_gen")
    val seq: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_info_seq", referencedColumnName = "seq")
    val accessInfo: AccessInfo,
    var time: LocalDateTime,
    @Column(length = 2000)
    var snapshot: String,
    var totalMoney: Long
) {
    fun update(minuteSnapshot: MinuteSnapshot, nextDay: LocalDateTime) {
        this.snapshot = minuteSnapshot.snapshot
        this.totalMoney = minuteSnapshot.totalMoney
        this.time = nextDay
    }

    constructor(minuteSnapshot: MinuteSnapshot) : this(
        accessInfo = minuteSnapshot.accessInfo,
        time = minuteSnapshot.time,
        snapshot = minuteSnapshot.snapshot,
        totalMoney = minuteSnapshot.totalMoney
    )
}
