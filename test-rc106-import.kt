import com.danim.autowarp.ReviewCsvImport
import com.danim.autowarp.ReviewLabels
fun main() {
 val items=listOf(ReviewLabels.Item("RC103-001","attack_001.wav","KICK"),ReviewLabels.Item("RC103-002","attack_002.wav","SNARE"))
 fun ok(csv:String, n:Int) { check(ReviewCsvImport.parse(csv,items).reviewed==n) }
 fun bad(csv:String) { check(runCatching { ReviewCsvImport.parse(csv,items) }.isFailure) }
 val header="review_id,audio_clip,historical_prediction,human_label\n"
 ok(header+"RC103-001,attack_001.wav,KICK,KICK\nRC103-002,attack_002.wav,SNARE,\n",1)
 ok("\uFEFF"+header+"\"RC103-001\",\"attack_001.wav\",\"KICK\",\"SNARE\"\r\n",1)
 ok(header+"RC103-001,attack_001.wav,KICK,UNCERTAIN\n",1)
 ok(header,0)
 bad(header+"RC103-001,wrong.wav,KICK,KICK\n")
 bad(header+"RC103-999,attack_001.wav,KICK,KICK\n")
 bad(header+"RC103-001,attack_001.wav,KICK,UNKNOWN\n")
 bad(header+"RC103-001,attack_001.wav,KICK,KICK\nRC103-001,attack_001.wav,KICK,SNARE\n")
 bad("review_id,human_label\nRC103-001,KICK\n")
 bad(header+"RC103-001,attack_001.wav,KICK,\"KICK\n")
 bad(header+"RC103-001,attack_001.wav,KICK,KICK,EXTRA\n")
 println("RC106: 11 pruebas de importación superadas")
}
