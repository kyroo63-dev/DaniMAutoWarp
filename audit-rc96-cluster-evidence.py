#!/usr/bin/env python3
"""Compare spectral/periodic evidence after anchored vs legacy clustering.
Diagnostic only: replays RC91 events, not fresh detection or ground-truth labels.
"""
import csv, math, sys
from collections import defaultdict
from pathlib import Path

def cluster(events, bpm, anchored):
    threshold = 60.0 / bpm * 0.55
    groups = []
    for e in sorted(events, key=lambda x:(x[0],-x[2])):
        if not groups or e[0] - (groups[-1][0][0] if anchored else groups[-1][-1][0]) >= threshold:
            groups.append([e])
        else:
            groups[-1].append(e)
    return [min(g, key=lambda x:(-x[2],x[0])) for g in groups]

def evaluate(events,bpm):
    period=60.0/bpm
    accepted=[]
    for i,(t,bass,spectral) in enumerate(events):
        neighbours=sum(1 for j,(u,_,_) in enumerate(events) if i!=j and
            1<=round(abs(t-u)/period)<=2 and
            abs(abs(t-u)/period-round(abs(t-u)/period))<=0.16)
        if bass>=0.65 and spectral>=0.60 and neighbours>=2:
            accepted.append(t)
    return accepted

def main(events_path,summary_path,out_path):
    by_offset=defaultdict(list)
    with open(events_path,newline='') as f:
        for r in csv.DictReader(f):
            if r['kind']=='KICK':
                by_offset[int(float(r['offset_s']))].append((float(r['event_s']),float(r['bass_share']),float(r['spectral_score'])))
    with open(summary_path,newline='') as f: segments=list(csv.DictReader(f))
    totals=[0]*7
    with open(out_path,'w',newline='') as f:
        w=csv.writer(f)
        w.writerow(['offset_s','raw_candidates','legacy_retained','anchored_retained','legacy_evidence_pass','anchored_evidence_pass','additional_anchored_pass','anchored_pass_ratio','note'])
        for row in segments:
            offset=int(float(row['offset_s'])); bpm=float(row['bpm']); raw=by_offset[offset]
            legacy=cluster(raw,bpm,False); anchored=cluster(raw,bpm,True)
            old_pass=evaluate(legacy,bpm); new_pass=evaluate(anchored,bpm)
            old_times=set(round(t,6) for t in old_pass)
            extra=sum(round(t,6) not in old_times for t in new_pass)
            nums=[len(raw),len(legacy),len(anchored),len(old_pass),len(new_pass),extra]
            totals[:6]=[a+b for a,b in zip(totals[:6],nums)]
            w.writerow([offset,*nums,f'{len(new_pass)/len(anchored):.3f}' if anchored else '', 'diagnostic_only_no_ground_truth'])
    print('totals raw legacy anchored legacy_pass anchored_pass additional_pass:',*totals[:6])
if __name__=='__main__': main(*sys.argv[1:])
