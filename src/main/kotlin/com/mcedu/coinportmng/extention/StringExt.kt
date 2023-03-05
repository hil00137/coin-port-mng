package com.mcedu.coinportmng.extention

fun String?.getInfoSeq(): Long {
    return try {
        this?.toLong() ?: throw NumberFormatException()
    } catch (ex: NumberFormatException) {
        throw RuntimeException("잘못된 번호입니다.")
    }
}

operator fun String.times(value: Int): String {
    val sb = StringBuilder()

    return if (value > 1) {
        for (i in 1 .. value) {
            sb.append(this)
        }
        sb.toString()
    } else {
        this
    }
}