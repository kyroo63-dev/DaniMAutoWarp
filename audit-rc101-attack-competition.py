#!/usr/bin/env python3
"""Recompute broad-window attack evidence near RC98 phase-match joins.
Historic KICK/OTHER labels are detector outputs, NOT verified ground truth.
Diagnostic only; never authorizes warp.
"""
import csv, subprocess, sys
from collections import defaultdict
import numpy as np

AUDIO, JOINS, EVENTS, OUTPUT = sys.argv[1:]
with open(JOINS, newline='') as f:
    joins=[r for r in csv.DictReader(f) if r['diagnostic_match'].lower()=='true']
events=defaultdict(list)
with open(EVENTS,newline='') as f:
    for r in csv.DictReader(f): events[float(r['offset_s'])].append(r)
SR=22050

def clip(start, length=8):
    p=subprocess.run(['ffmpeg','-nostdin','-v','error','-ss',str(start),'-i',AUDIO,'-t',str(length),'-ac','1','-ar',str(SR),'-f','f32le','pipe:1'],capture_output=True,check=True)
    return np.frombuffer(p.stdout,dtype='<f4')

def features(audio, t):
    c=round(t*SR)
    # 186 ms analysis for spectral stability, with 40 ms pre-onset and 140 ms post-onset energy.
    n=4096; a=c-1024; b=a+n
    pre0=c-round(.045*SR); post1=c+round(.14*SR)
    if min(a,pre0)<0 or max(b,post1)>len(audio): return None
    x=audio[a:b].astype(np.float64)*np.hanning(n)
    power=np.abs(np.fft.rfft(x))**2
    hz=np.fft.rfftfreq(n,1/SR)
    mask=(hz>=30)&(hz<5000)
    total=power[mask].sum()
    if total<=1e-12:return None
    bass=power[(hz>=30)&(hz<160)].sum()/total
    centroid=(hz[mask]*power[mask]).sum()/total
    pre=np.mean(np.square(audio[pre0:c].astype(np.float64)))
    post=np.mean(np.square(audio[c:post1].astype(np.float64)))
    attack_db=10*np.log10((post+1e-10)/(pre+1e-10))
    return float(bass),float(centroid),float(attack_db)

def median(items, index):
    return round(float(np.median([v[index] for v in items])),5) if items else ''

rows=[]
for join in joins:
    left=float(join['left_start_s']); right=float(join['right_start_s'])
    for side,offset,origin in [('left',left,right-8),('right',right,right)]:
        audio=clip(origin)
        measured=[]
        for e in events[offset]:
            t=float(e['absolute_event_s'])-origin
            if not .20<=t<=7.75:continue
            f=features(audio,t)
            if f is not None: measured.append((e['kind'],f))
        kick=[v for kind,v in measured if kind=='KICK']
        other=[v for kind,v in measured if kind!='KICK']
        # Overlap of bass-rich attacks: spectral bass alone cannot identify KICK reliably.
        kick_rich=sum(v[0]>=.5 for v in kick)
        other_rich=sum(v[0]>=.5 for v in other)
        rows.append(dict(join_s=right,side=side,window_start_s=origin,
            detector_kick_count=len(kick),detector_other_count=len(other),
            kick_bass_median=median(kick,0),other_bass_median=median(other,0),
            kick_centroid_hz_median=median(kick,1),other_centroid_hz_median=median(other,1),
            kick_attack_db_median=median(kick,2),other_attack_db_median=median(other,2),
            kick_bass_rich_count=kick_rich,other_bass_rich_count=other_rich,
            competing_bass_rich_attacks=other_rich>0,
            verified_kick_identity=False,warp_authorized=False))
with open(OUTPUT,'w',newline='') as f:
    writer=csv.DictWriter(f,fieldnames=list(rows[0]));writer.writeheader();writer.writerows(rows)
print('windows:',len(rows),'historic KICK:',sum(r['detector_kick_count'] for r in rows),
      'other:',sum(r['detector_other_count'] for r in rows),
      'windows with competing bass-rich attacks:',sum(r['competing_bass_rich_attacks'] for r in rows))
