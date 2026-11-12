package com.danim.autowarp

/** Pure review navigation and import planning; no Android dependencies. */
object ReviewProgress {
    fun restoreIndex(savedId: String?, items: List<ReviewLabels.Item>): Int =
        items.indexOfFirst { it.id == savedId }.coerceAtLeast(0)

    fun nextIndex(current: Int, count: Int): Int {
        require(count > 0 && current in 0 until count)
        return (current + 1).coerceAtMost(count - 1)
    }

    fun previousIndex(current: Int, count: Int): Int {
        require(count > 0 && current in 0 until count)
        return (current - 1).coerceAtLeast(0)
    }

    fun merge(existing: Map<String, String>, imported: Map<String, String>, items: List<ReviewLabels.Item>): Map<String, String> {
        val ids = items.map { it.id }.toSet()
        require(imported.keys.all { it in ids })
        require(imported.values.all { it in ReviewLabels.allowed })
        return existing + imported
    }
}
