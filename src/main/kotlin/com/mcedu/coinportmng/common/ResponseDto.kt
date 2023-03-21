package com.mcedu.coinportmng.common

data class ResponseDto<T>(
    val data: T?,
    val status: Int = 200,
    val message: String = ""
) {
    companion object {
        fun success(msg: String): ResponseDto<Nothing> {
            return ResponseDto(data = null, message = msg)
        }

        fun fail(message: String): ResponseDto<Nothing> {
            return ResponseDto(data = null, message = message, status = 500)
        }
    }
}