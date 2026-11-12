#!/usr/bin/env python3
import importlib.util
from pathlib import Path
p=Path(__file__).with_name('audit-rc97-rhythm-runs.py')
s=importlib.util.spec_from_file_location('audit_rc97',p)
m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
period=0.5
assert m.runs([],120)==[]
assert m.runs([0.0],120)==[[0.0]]
assert len(m.runs([0,.5,1,1.5],120))==1
assert len(m.runs([0,.5,1,3.5,4],120))==2
assert len(m.runs([0,.5,1,1.28,1.78],120))==2
assert len(m.runs([0,.5,1,2,2.5],120))==1
assert m.inspect([0,.5,1,1.5],120,24)['longest_run']==4
assert not m.inspect([0,.5,1,1.5],120,24)['diagnostic_stable']
assert m.inspect([i*.5 for i in range(14)],120,24)['diagnostic_stable']
assert not m.inspect([],120,24)['diagnostic_stable']
assert m.filter_candidates([],120)==[]
assert m.filter_candidates([(0,.9,.9),(.5,.9,.9),(1,.9,.9)],120)==[0,.5,1]
print('RC97: 12 synthetic checks passed')
