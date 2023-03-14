package com.mcedu.coinportmng.controller

import com.mcedu.coinportmng.common.ResponseDto
import com.mcedu.coinportmng.extention.getInfoSeq
import com.mcedu.coinportmng.service.SessionService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class LoginController(private val sessionService: SessionService) {

    @PostMapping("/login/{seq}")
    fun login(@PathVariable seq: String?): ResponseDto<Nothing> {
        sessionService.login(seq.getInfoSeq())
        return ResponseDto.success("로그인 성공")
    }

    @PostMapping("/logout")
    fun logOut(): ResponseDto<Nothing> {
        sessionService.logout()
        return ResponseDto.success("로그아웃 성공")
    }
}