package com.mcedu.coinportmng.entity

import jakarta.persistence.*
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@SequenceGenerator(
    name = Sequence.MINUTE_SNAPSHOT + "_gen",
    sequenceName = Sequence.MINUTE_SNAPSHOT,
    initialValue = 1,
    allocationSize = 1
)
@DynamicInsert
@DynamicUpdate
@EntityListeners(AuditingEntityListener::class)
@Table(name = "minute_snapshot", indexes = [Index(name = "idx_minute_snapshot", columnList = "access_info_seq, time")])
data class MinuteSnapshot(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = Sequence.MINUTE_SNAPSHOT + "_gen")
    val seq: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_info_seq", referencedColumnName = "seq")
    val accessInfo: AccessInfo,
    val time: LocalDateTime,
    @Column(length = 2000)
    val snapshot: String,
    val totalMoney: Long
)
