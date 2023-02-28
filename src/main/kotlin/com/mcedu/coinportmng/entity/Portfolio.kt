package com.mcedu.coinportmng.entity

import jakarta.persistence.*
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@SequenceGenerator(
    name = Sequence.PORTFOLIO + "_gen",
    sequenceName = Sequence.PORTFOLIO,
    initialValue = 1,
    allocationSize = 1
)
@DynamicInsert
@DynamicUpdate
@EntityListeners(AuditingEntityListener::class)
@Table(name = "portfolio")
data class Portfolio(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = Sequence.PORTFOLIO + "_gen")
    val seq: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_info_seq", referencedColumnName = "seq")
    val accessInfo: AccessInfo,
    val ticker: String,
    var ratio: Double,
    @CreatedDate
    val createdDate: LocalDateTime = LocalDateTime.now()
)