import importlib.util
from pathlib import Path
spec=importlib.util.spec_from_file_location('rc99',Path(__file__).with_name('audit-rc99-spectral-joins.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
from collections import defaultdict
j=dict(left_start_s='0',right_start_s='24',diagnostic_match='True')
def ev(t,kind='KICK',bass=.9,spectral=.9):return dict(absolute=t,kind=kind,bass=bass,spectral=spectral)
d=defaultdict(list)
for i in range(4):
 d[0.0].append(ev(20+i));d[24.0].append(ev(24+i))
r=m.corroborate(j,d)
assert r['spectral_corroboration'] and not r['warp_authorized']
assert r['left_kick_evidence']==4 and r['right_kick_evidence']==4
assert r['spectral_decision']=='corroborated_diagnostic_only'
d[0.0].extend(ev(20+i,'SNARE') for i in range(5))
assert not m.corroborate(j,d)['spectral_corroboration']
d[0.0]=[ev(20+i,bass=.6) for i in range(4)]
assert not m.corroborate(j,d)['spectral_corroboration']
assert m.corroborate(dict(j,diagnostic_match='False'),d)['spectral_decision']=='not_phase_eligible'
assert m.corroborate(j,defaultdict(list))['spectral_decision']=='ambiguous_spectral_evidence'
assert m.corroborate(j,defaultdict(list))['left_bass_median']==''
print('RC99: 8 synthetic assertions passed')
