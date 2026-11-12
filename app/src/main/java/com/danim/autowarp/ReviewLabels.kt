package com.danim.autowarp

/** Human review is ground truth only after explicit user labeling; historical labels are predictions. */
object ReviewLabels {
    val allowed = setOf("KICK", "SNARE", "PERCUSSION", "UNCERTAIN")
    data class Item(val id: String, val clip: String, val prediction: String)
    fun parseQueue(csv: String): List<Item> {
        val lines = csv.lineSequence().filter { it.isNotBlank() }.toList()
        require(lines.firstOrNull()?.startsWith("review_id,") == true)
        return lines.drop(1).map { line ->
            val cells = line.split(',')
            require(cells.size >= 11 && cells[0].matches(Regex("RC103-[0-9]{3}")))
            require(cells[10].matches(Regex("attack_[0-9]{3}\\.wav")))
            Item(cells[0], cells[10], cells[4])
        }
    }
    fun escape(value: String) = "\"" + value.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\""
    fun export(items: List<Item>, labels: Map<String,String>): String {
        require(labels.keys.all { id -> items.any { it.id == id } })
        require(labels.values.all { it in allowed })
        return buildString {
            appendLine("review_id,audio_clip,historical_prediction,human_label")
            items.forEach { appendLine(listOf(it.id,it.clip,it.prediction,labels[it.id].orEmpty()).joinToString(",") { v -> escape(v) }) }
        }
    }
    data class Metrics(val tp: Int, val fp: Int, val fn: Int, val tn: Int) {
        val precision get() = if (tp+fp == 0) null else tp.toDouble()/(tp+fp)
        val recall get() = if (tp+fn == 0) null else tp.toDouble()/(tp+fn)
    }
    fun evaluate(items: List<Item>, labels: Map<String,String>): Metrics? {
        val reviewed = items.filter { labels[it.id] in allowed && labels[it.id] != "UNCERTAIN" }
        if (reviewed.isEmpty()) return null
        return Metrics(reviewed.count { it.prediction == "KICK" && labels[it.id] == "KICK" },
            reviewed.count { it.prediction == "KICK" && labels[it.id] != "KICK" },
            reviewed.count { it.prediction != "KICK" && labels[it.id] == "KICK" },
            reviewed.count { it.prediction != "KICK" && labels[it.id] != "KICK" })
    }
}
