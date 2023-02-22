package com.mcedu.coinportmng.entity

import jakarta.persistence.*

@Entity
@SequenceGenerator(
    name = "temp_entity_seq_gen",
    sequenceName = "temp_entity_seq",
    initialValue = 1,
    allocationSize = 1
)
class TempEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "temp_entity_seq_gen")
    val id: Long? = null,
    val name: String
)