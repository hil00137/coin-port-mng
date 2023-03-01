package com.mcedu.coinportmng.entity

import jakarta.persistence.*
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@SequenceGenerator(
    name = Sequence.REBALANCE_MNG + "_gen",
    sequenceName = Sequence.REBALANCE_MNG,
    initialValue = 1,
    allocationSize = 1
)
@DynamicInsert
@DynamicUpdate
@EntityListeners(AuditingEntityListener::class)
@Table(name = "rebalance_mng")
data class RebalanceMng(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = Sequence.REBALANCE_MNG + "_gen")
    val seq: Long? = null,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_info_seq", referencedColumnName = "seq")
    val accessInfo: AccessInfo,
    val baseMonth: Int,
    val baseDay: Int,
    val baseTime: Int,
    @Column(name = "execute_interval")
    val interval: String,
    val bandRebalance: Boolean = false,
    val bandCheck: Double? = null,
    val active: Boolean = false,
    @CreatedDate
    val createdDate: LocalDateTime,
    @LastModifiedDate
    val updatedDate: LocalDateTime
)
