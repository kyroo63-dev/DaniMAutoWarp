from pathlib import Path
s=(Path(__file__).parent/'app/src/main/java/com/danim/autowarp/ReviewActivity.kt').read_text()
checks=[
('guard empty queue','require(items.isNotEmpty())' in s),
('synchronous single-label commit','if (prefs.edit().putString(current.id, value).commit())' in s),
('advance only on successful save','labels[current.id] = value\n                                index = ReviewProgress.nextIndex' in s),
('import merged snapshot','merged.forEach { (id, value) -> editor.putString(id, value) }' in s),
('commit import before updating UI','if (!editor.commit())' in s and s.index('if (!editor.commit())') < s.index('labels.clear()')),
('release safely','old?.runCatching { release() }' in s),
('completion callback identity check','if (player === completed) stopPlayback()' in s),
('audio error callback','setOnErrorListener' in s),
('stop on lifecycle','override fun onStop()' in s),
]
for name,ok in checks:
    assert ok,name
print(f'RC110: {len(checks)} comprobaciones estáticas superadas')
