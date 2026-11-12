from pathlib import Path
root=Path(__file__).parent
s=(root/'app/src/main/java/com/danim/autowarp/ReviewActivity.kt').read_text()
checks={
 'scrollable content':'.verticalScroll(rememberScrollState())' in s,
 'player released on index change':'DisposableEffect(index)' in s and 'onDispose { player?.release(); player = null }' in s,
 'player released on background':'override fun onStop() { player?.release(); player = null;' in s,
 'import document picker':'ActivityResultContracts.OpenDocument()' in s,
 'export document picker':'ActivityResultContracts.CreateDocument' in s,
 'persistent labels':'getSharedPreferences(' in s,
 'audio preview':'MediaPlayer()' in s,
 'waveform':'Canvas(' in s,
 'classification options':'"UNCERTAIN" to "INCIERTO"' in s,
}
for k,v in checks.items():
 print(('PASS' if v else 'FAIL'),k)
assert all(checks.values())
