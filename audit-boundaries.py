#!/usr/bin/env python3
"""Diagnostic only; uses independent clip onset candidates, not verified kick annotations."""
import csv, math, sys, collections
summary, events, output = sys.argv[1:4]
with open(summary,newline='') as f: segments=list(csv.DictReader(f))
with open(events,newline='') as f: raw=list(csv.DictReader(f))
kicks=collections.defaultdict(list)
for e in raw:
    if e['kind']=='KICK': kicks[int(float(e['offset_s']))].append(float(e['event_s']))
def phase(times,bpm):
    if not times or not 60<=bpm<=240:return float('nan')
    period=60/bpm; tolerance=period*.16
    candidates=[t%period for t in times]
    def error(t,p):return abs((t-p)-round((t-p)/period)*period)
    def quality(p):
        hits=[t for t in times if error(t,p)<=tolerance]
        return (len(set(round((t-p)/period) for t in hits)),-sum(error(t,p) for t in hits))
    return max(candidates,key=quality)
rows=[]
for a,b in zip(segments,segments[1:]):
    start_a=float(a['start_s']);start_b=float(b['start_s'])
    bpm_a=float(a['bpm']);bpm_b=float(b['bpm'])
    p_a=phase(kicks[int(start_a)],bpm_a);p_b=phase(kicks[int(start_b)],bpm_b)
    contiguous=abs(float(a['end_s'])-start_b)<=.002
    # Only production-authorized isolated clips are eligible; never enable production with this audit.
    eligible=contiguous and a['automatic_warp_in_isolated_clip']=='true' and b['automatic_warp_in_isolated_clip']=='true'
    jump=abs(bpm_a-bpm_b)/((bpm_a+bpm_b)/2)
    period=60/((bpm_a+bpm_b)/2)
    delta=(start_a+p_a)-(start_b+p_b)
    phase_error=abs(delta-round(delta/period)*period)
    continuous=eligible and jump<=.025 and phase_error<=period*.16
    rows.append(dict(left_start_s=start_a,right_start_s=start_b,eligible=eligible,tempo_jump_fraction=round(jump,5),phase_jump_s=round(phase_error,5),continuous=continuous,decision='diagnostic_only'))
with open(output,'w',newline='') as f:
    w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
print(f'boundaries={len(rows)} eligible={sum(r["eligible"] for r in rows)} continuous={sum(r["continuous"] for r in rows)}')
