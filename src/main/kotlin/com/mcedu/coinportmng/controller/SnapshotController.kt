package com.mcedu.coinportmng.controller

import com.mcedu.coinportmng.dto.SnapshotDto
import com.mcedu.coinportmng.service.SnapshotService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class SnapshotController(
    private val snapshotService: SnapshotService
) {

    @GetMapping("/v1/snapshot")
    fun getSnapshot(@RequestParam q: String): List<SnapshotDto> {
        return snapshotService.getSnapshot(q)
    }
}