#!/usr/bin/env python3
"""Diagnostic-only cross-clip phase audit, not a global warp or verified kick identity."""
import csv, math, sys, importlib.util
from collections import defaultdict
from pathlib import Path
spec=importlib.util.spec_from_file_location('rhythm',Path(__file__).with_name('audit-rc97-rhythm-runs.py'))
r=importlib.util.module_from_spec(spec);spec.loader.exec_module(r)

def phase_distance(a,b,period):
    delta=(a-b)%period
    return min(delta,period-delta)

def boundary(left,right):
    end=left['start']+left['duration']; start=right['start']
    result=dict(left_start_s=left['start'],right_start_s=start,contiguous=abs(end-start)<0.05,
                left_stable=left['stable'],right_stable=right['stable'],left_edge_events=0,right_edge_events=0,
                bpm_jump_fraction='',phase_jump_s='',phase_tolerance_s='',diagnostic_match=False,
                decision='inconclusive',warp_authorized=False)
    if not result['contiguous']:
        result['decision']='noncontiguous';return result
    if not left['stable'] or not right['stable']:
        result['decision']='insufficient_stability';return result
    l=[t+left['start'] for t in left['times'] if t>=left['duration']-4]
    rr=[t+start for t in right['times'] if t<=4]
    result['left_edge_events']=len(l);result['right_edge_events']=len(rr)
    if len(l)<4 or len(rr)<4:
        result['decision']='insufficient_edge_events';return result
    bpm_jump=abs(left['bpm']-right['bpm'])/max(left['bpm'],right['bpm'])
    result['bpm_jump_fraction']=round(bpm_jump,5)
    if bpm_jump>0.025:
        result['decision']='tempo_mismatch';return result
    period=60/((left['bpm']+right['bpm'])/2)
    # Robust circular phase fit; use median residual across all pairs of edge events.
    distances=sorted(phase_distance(x,y,period) for x in l for y in rr)
    phase=distances[len(distances)//2]
    tolerance=min(.055,period*.12)
    result['phase_jump_s']=round(phase,5);result['phase_tolerance_s']=round(tolerance,5)
    result['diagnostic_match']=phase<=tolerance
    result['decision']='phase_consistent_diagnostic_only' if result['diagnostic_match'] else 'phase_mismatch'
    return result

def audit(events_path,summary_path,output_path):
    events=defaultdict(list)
    with open(events_path,newline='') as f:
        for row in csv.DictReader(f):
            if row['kind']=='KICK':
                events[int(float(row['offset_s']))].append((float(row['event_s']),float(row['bass_share']),float(row['spectral_score'])))
    segments=[]
    with open(summary_path,newline='') as f:
        for row in csv.DictReader(f):
            start=int(float(row['offset_s']));bpm=float(row['bpm']);duration=float(row['duration_s'])
            times=r.filter_candidates(r.cluster(events[start],bpm),bpm)
            segments.append(dict(start=start,bpm=bpm,duration=duration,times=times,stable=r.inspect(times,bpm,duration)['diagnostic_stable']))
    segments.sort(key=lambda s:s['start'])
    results=[boundary(a,b) for a,b in zip(segments,segments[1:])]
    with open(output_path,'w',newline='') as f:
        writer=csv.DictWriter(f,fieldnames=list(results[0]));writer.writeheader();writer.writerows(results)
    from collections import Counter
    print('joins',len(results),'decisions',dict(Counter(x['decision'] for x in results)),'warp_authorized',0)
if __name__=='__main__':audit(*sys.argv[1:])
