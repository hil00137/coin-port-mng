package com.mcedu.coinportmng.service

import com.mcedu.coinportmng.entity.TempEntity
import com.mcedu.coinportmng.repository.TestRepository
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service

@Service
class TestService(
    private val testRepository: TestRepository
) {
    @PostConstruct
    fun init() {
        testRepository.save(TempEntity(name = "hello"))
    }
}