package com.mcedu.coinportmng.common

data class ResponseDto<T>(
    val data: T?,
    val message: String = ""
) {
    companion object {
        fun success(msg: String): ResponseDto<Nothing> {
            return ResponseDto(data = null, message = msg)
        }
    }
}