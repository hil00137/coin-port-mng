package com.mcedu.coinportmng.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping
class ViewController {

    @GetMapping("/{depth1}")
    fun page(@PathVariable depth1: String): String {
        return depth1
    }
}