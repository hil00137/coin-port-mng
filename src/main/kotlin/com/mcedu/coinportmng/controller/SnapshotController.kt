package com.mcedu.coinportmng.controller

import com.mcedu.coinportmng.dto.SnapshotDto
import com.mcedu.coinportmng.repository.DaySnapshotRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class SnapshotController(
    private val daySnapshotRepository: DaySnapshotRepository
) {

    @GetMapping("/v1/snapshot")
    fun getSnapshot(): List<SnapshotDto> {
        return daySnapshotRepository.findAll().map { SnapshotDto(time = it.time, totalMoney = it.totalMoney) }
    }
}