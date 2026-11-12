import importlib.util
from pathlib import Path
p=Path(__file__).with_name('audit-rc98-phase-joins.py');s=importlib.util.spec_from_file_location('phase',p);m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
def seg(start,phase=0,bpm=120,stable=True,duration=24):
    return dict(start=start,duration=duration,bpm=bpm,stable=stable,times=[phase+i*.5 for i in range(48)])
a=seg(0);b=seg(24)
assert m.boundary(a,b)['diagnostic_match']
assert not m.boundary(a,b)['warp_authorized']
assert m.boundary(a,seg(24,.22))['decision']=='phase_mismatch'
assert m.boundary(a,seg(24,bpm=130))['decision']=='tempo_mismatch'
assert m.boundary(a,seg(25))['decision']=='noncontiguous'
assert m.boundary(a,seg(24,stable=False))['decision']=='insufficient_stability'
assert m.boundary(seg(0,duration=24),seg(24,duration=2))['decision']=='phase_consistent_diagnostic_only'
assert abs(m.phase_distance(.49,.01,.5)-.02)<1e-9
assert m.phase_distance(0,0,.5)==0
assert m.phase_distance(.25,0,.5)==.25
print('RC98: 10 synthetic checks passed')
