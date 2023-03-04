package com.mcedu.coinportmng.entity

import com.mcedu.coinportmng.type.UpbitIndex
import jakarta.persistence.*
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@SequenceGenerator(
    name = Sequence.INDEX_INfO + "_gen",
    sequenceName = Sequence.INDEX_INfO,
    initialValue = 1,
    allocationSize = 1
)
@DynamicInsert
@DynamicUpdate
@EntityListeners(AuditingEntityListener::class)
data class UpbitIndexInfo(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = Sequence.INDEX_INfO + "_gen")
    val seq: Long? = null,
    @Enumerated(EnumType.STRING)
    val name: UpbitIndex,
    @Column(length = 2000)
    var detailJson: String,
    @LastModifiedDate
    val updateDate: LocalDateTime = LocalDateTime.now()
)
