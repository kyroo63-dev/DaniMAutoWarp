import com.danim.autowarp.ReviewLabels
fun main() {
 val rows = ReviewLabels.parseQueue("review_id,join_s,side,event_s,historical_label,fusion_candidate,bass_share,centroid_hz,attack_db,rhythm_neighbours,audio_clip,human_label,reviewer,review_notes\nRC103-001,72,left,1,KICK,True,0.8,120,2,3,attack_001.wav,,,\nRC103-002,72,right,2,SNARE,False,0.2,1200,2,0,attack_002.wav,,,")
 check(rows.size==2)
 check(ReviewLabels.evaluate(rows,emptyMap())==null)
 val labels=mapOf("RC103-001" to "SNARE", "RC103-002" to "KICK")
 val m=ReviewLabels.evaluate(rows,labels)!!
 check(m.tp==0 && m.fp==1 && m.fn==1 && m.tn==0)
 check(m.precision==0.0 && m.recall==0.0)
 check(ReviewLabels.export(rows,labels).lines().size==4)
 check(ReviewLabels.escape("a\"b") == "\"a\"\"b\"")
 check(runCatching { ReviewLabels.export(rows,mapOf("RC103-001" to "INVALID")) }.isFailure)
 println("RC105: 7 comprobaciones OK")
}
