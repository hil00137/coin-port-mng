package com.mcedu.coinportmng.extention

fun String?.getInfoSeq(): Long {
    return try {
        this?.toLong() ?: throw NumberFormatException()
    } catch (ex: NumberFormatException) {
        throw RuntimeException("잘못된 번호입니다.")
    }
}