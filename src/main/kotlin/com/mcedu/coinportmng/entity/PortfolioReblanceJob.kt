package com.mcedu.coinportmng.entity

import com.mcedu.coinportmng.common.ReblanceJobStatus
import jakarta.persistence.*
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@SequenceGenerator(
    name = Sequence.PORTFOLIO_REBALANCE_JOB + "_gen",
    sequenceName = Sequence.PORTFOLIO_REBALANCE_JOB,
    initialValue = 1,
    allocationSize = 1
)
@DynamicInsert
@DynamicUpdate
@EntityListeners(AuditingEntityListener::class)
@Table(name = "portfolio_rebalance_job")
data class PortfolioRebalanceJob(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = Sequence.PORTFOLIO_REBALANCE_JOB + "_gen")
    val seq: Long? = null,
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "access_info_seq", referencedColumnName = "seq")
    val accessInfo: AccessInfo,
    @Enumerated(EnumType.STRING)
    var status: ReblanceJobStatus,
    @CreatedDate
    val createdDate: LocalDateTime = LocalDateTime.now()
)