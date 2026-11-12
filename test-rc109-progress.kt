import com.danim.autowarp.ReviewLabels
import com.danim.autowarp.ReviewProgress

fun main() {
    val items = listOf(ReviewLabels.Item("a","a.wav","KICK"), ReviewLabels.Item("b","b.wav","SNARE"))
    check(ReviewProgress.restoreIndex("b",items) == 1)
    check(ReviewProgress.restoreIndex("missing",items) == 0)
    check(ReviewProgress.restoreIndex(null,items) == 0)
    check(ReviewProgress.nextIndex(0,2) == 1)
    check(ReviewProgress.nextIndex(1,2) == 1)
    check(ReviewProgress.previousIndex(1,2) == 0)
    check(ReviewProgress.previousIndex(0,2) == 0)
    val merged = ReviewProgress.merge(mapOf("a" to "KICK"),mapOf("b" to "SNARE"),items)
    check(merged.size == 2 && merged["a"] == "KICK")
    check(runCatching { ReviewProgress.merge(emptyMap(),mapOf("z" to "KICK"),items) }.isFailure)
    check(runCatching { ReviewProgress.merge(emptyMap(),mapOf("a" to "INVALID"),items) }.isFailure)
    println("RC109: 10 pruebas JVM correctas")
}
