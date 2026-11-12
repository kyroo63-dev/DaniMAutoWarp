import com.danim.autowarp.ReviewWav
import java.io.File
import java.io.ByteArrayOutputStream

fun checkFails(data: ByteArray) { check(runCatching { ReviewWav.peaks(data) }.isFailure) }
fun main(args: Array<String>) {
    val real = File(args[0]).readBytes()
    val p = ReviewWav.peaks(real)
    check(p.size==200 && p.all { it in 0f..1f } && p.any { it>0f })
    checkFails(byteArrayOf(1,2,3))
    checkFails(real.copyOf(24))
    val bad = real.copyOf(); bad[20]=3; bad[21]=0; checkFails(bad)
    val truncated = real.copyOf(real.size-100); checkFails(truncated)
    val wrongChannels=real.copyOf(); wrongChannels[22]=3; wrongChannels[23]=0; checkFails(wrongChannels)
    check(runCatching { ReviewWav.peaks(real,0) }.isFailure)
    check(runCatching { ReviewWav.peaks(real,4097) }.isFailure)
    val padded = ByteArrayOutputStream().apply {
        write(real.copyOfRange(0,12))
        write("JUNK".toByteArray()); write(byteArrayOf(1,0,0,0,42,0))
        write(real.copyOfRange(12,real.size))
    }.toByteArray()
    val newRiffSize=padded.size-8
    for(i in 0..3) padded[i+4]=(newRiffSize ushr (8*i)).toByte()
    check(ReviewWav.peaks(padded)==p)
    println("RC108: 9 comprobaciones PCM16/RIFF correctas")
}
