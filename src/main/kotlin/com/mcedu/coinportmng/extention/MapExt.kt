package com.mcedu.coinportmng.extention

fun Map<String, String>.logForm(): String {
    val entryList = this.entries.sortedWith(Comparator { o1, o2 ->
        val indexer: (String) -> Int = {
            if (it == "KRW") {
                1
            } else if (it.startsWith("IDX.")) {
                3
            } else {
                2
            }
        }
        val index1 = indexer(o1.key)
        val index2 = indexer(o2.key)
        return@Comparator if (index1 == index2) o1.key.compareTo(o2.key) else index1.compareTo(index2)
    })
    return entryList.toString().replace("[", "").replace("]", "")
}