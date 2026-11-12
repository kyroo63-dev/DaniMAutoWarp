package com.danim.autowarp

/** Validated, atomic import of human labels. Never treats historical predictions as ground truth. */
object ReviewCsvImport {
    data class Result(val labels: Map<String, String>, val reviewed: Int)

    private fun rows(csv: String): List<List<String>> {
        val output = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var afterQuote = false
        var i = 0
        while (i < csv.length) {
            val c = csv[i]
            if (quoted) {
                when {
                    c == '"' && i + 1 < csv.length && csv[i + 1] == '"' -> { cell.append('"'); i++ }
                    c == '"' -> { quoted = false; afterQuote = true }
                    else -> cell.append(c)
                }
            } else {
                when (c) {
                    '"' -> {
                        require(!afterQuote && cell.isEmpty()) { "Comillas CSV inválidas" }
                        quoted = true
                    }
                    ',' -> { row.add(cell.toString()); cell.clear(); afterQuote = false }
                    '\n' -> {
                        row.add(cell.toString().removeSuffix("\r")); cell.clear()
                        if (row.any { it.isNotEmpty() }) output.add(row)
                        row = mutableListOf(); afterQuote = false
                    }
                    else -> {
                        require(!afterQuote || c == '\r') { "Texto después de comillas" }
                        if (c != '\r') cell.append(c)
                    }
                }
            }
            i++
        }
        require(!quoted) { "Comillas CSV sin cerrar" }
        row.add(cell.toString())
        if (row.any { it.isNotEmpty() }) output.add(row)
        return output
    }

    fun parse(csv: String, items: List<ReviewLabels.Item>): Result {
        val lines = rows(csv.removePrefix("\uFEFF"))
        require(lines.isNotEmpty()) { "CSV vacío" }
        val header = lines.first()
        require(header.distinct().size == header.size) { "Columnas duplicadas" }
        val idCol = header.indexOf("review_id")
        val clipCol = header.indexOf("audio_clip")
        val labelCol = header.indexOf("human_label")
        require(idCol >= 0 && clipCol >= 0 && labelCol >= 0) { "Faltan columnas obligatorias" }
        val known = items.associateBy { it.id }
        val labels = linkedMapOf<String, String>()
        val seen = mutableSetOf<String>()
        for (line in lines.drop(1)) {
            require(line.size == header.size) { "Número de columnas incorrecto" }
            val id = line[idCol]
            require(seen.add(id)) { "ID duplicado: $id" }
            val item = known[id] ?: error("ID desconocido: $id")
            require(line[clipCol] == item.clip) { "Audio no coincide para $id" }
            val label = line[labelCol].trim().uppercase()
            require(label.isEmpty() || label in ReviewLabels.allowed) { "Etiqueta inválida: $label" }
            if (label.isNotEmpty()) labels[id] = label
        }
        return Result(labels, labels.size)
    }
}
