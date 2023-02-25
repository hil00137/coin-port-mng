package com.mcedu.coinportmng.entity

import com.mcedu.coinportmng.type.CoinRepositoryType
import jakarta.persistence.*
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@SequenceGenerator(
    name = Sequence.UPBIT_ACCESS_INFO + "_gen",
    sequenceName = Sequence.UPBIT_ACCESS_INFO,
    initialValue = 1,
    allocationSize = 1
)
@DynamicInsert
@DynamicUpdate
@EntityListeners(AuditingEntityListener::class)
@Table(name = "access_info")
data class AccessInfo(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = Sequence.UPBIT_ACCESS_INFO + "_gen")
    val seq: Long?,
    @Column(name = "name", length = 20)
    val name: String,
    @Column(name = "access_key", length = 50)
    val accessKey: String,
    @Column(name = "secret_key", length = 50)
    val secretKey: String,
    @Column(name = "expire_date_time")
    val expireDateTime: LocalDateTime,
    @Enumerated(EnumType.STRING)
    @Column(name = "repository_type", length = 20)
    val repositoryType: CoinRepositoryType,
    @CreatedDate
    @Column(name = "created_date")
    val createdDate: LocalDateTime
)