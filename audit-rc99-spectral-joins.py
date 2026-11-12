#!/usr/bin/env python3
"""Diagnostic spectral corroboration of RC98 joins. Historic RC91 features, not ground truth."""
import csv,sys,statistics
from collections import defaultdict

def corroborate(join, events):
    result=dict(join)
    result.update(left_kick_evidence=0,right_kick_evidence=0,left_competing_attacks=0,right_competing_attacks=0,
                  left_bass_median='',right_bass_median='',spectral_corroboration=False,
                  spectral_decision='not_phase_eligible',warp_authorized=False)
    if str(join['diagnostic_match']).lower()!='true':return result
    left=float(join['left_start_s']);right=float(join['right_start_s'])
    boundaries=[(left,right-4,right),(right,right,right+4)]
    for side,(offset,lo,hi) in zip(('left','right'),boundaries):
        candidates=[e for e in events[offset] if lo<=e['absolute']<=hi]
        kicks=[e for e in candidates if e['kind']=='KICK' and e['bass']>=.65 and e['spectral']>=.60]
        competing=[e for e in candidates if e['kind']!='KICK' and e['bass']>=.65 and e['spectral']>=.60]
        result[side+'_kick_evidence']=len(kicks)
        result[side+'_competing_attacks']=len(competing)
        result[side+'_bass_median']=round(statistics.median(e['bass'] for e in kicks),5) if kicks else ''
    # No verified kick labels or independent spectral measurement: diagnostic only.
    ok=(result['left_kick_evidence']>=4 and result['right_kick_evidence']>=4 and
        result['left_competing_attacks']<=result['left_kick_evidence'] and
        result['right_competing_attacks']<=result['right_kick_evidence'] and
        min(result['left_bass_median'] or 0,result['right_bass_median'] or 0)>=.75)
    result['spectral_corroboration']=ok
    result['spectral_decision']='corroborated_diagnostic_only' if ok else 'ambiguous_spectral_evidence'
    return result

def audit(join_file,event_file,output):
    events=defaultdict(list)
    with open(event_file,newline='') as f:
        for r in csv.DictReader(f):
            events[float(r['offset_s'])].append(dict(absolute=float(r['absolute_event_s']),kind=r['kind'],bass=float(r['bass_share']),spectral=float(r['spectral_score'])))
    with open(join_file,newline='') as f: rows=[corroborate(r,events) for r in csv.DictReader(f)]
    with open(output,'w',newline='') as f:
        w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
    print('joins',len(rows),'phase_matches',sum(str(r['diagnostic_match']).lower()=='true' for r in rows),'spectrally_corroborated',sum(r['spectral_corroboration'] for r in rows),'warp_authorized',0)
if __name__=='__main__':audit(*sys.argv[1:])
