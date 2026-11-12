#!/usr/bin/env python3
"""Re-measure spectral evidence from original FLAC near RC98 phase-match joins.
Diagnostic only. Candidate timestamps and KICK labels come from historic detector.
"""
import csv, subprocess, sys, numpy as np
from collections import defaultdict

AUDIO, JOINS, EVENTS, OUTPUT = sys.argv[1:]
with open(JOINS, newline='') as f:
    joins = [r for r in csv.DictReader(f) if r['diagnostic_match'].lower() == 'true']
events=defaultdict(list)
with open(EVENTS,newline='') as f:
    for r in csv.DictReader(f):
        events[float(r['offset_s'])].append(r)

SR=22050

def clip(start, length=4):
    p=subprocess.run(['ffmpeg','-nostdin','-v','error','-ss',str(start),'-i',AUDIO,'-t',str(length),'-ac','1','-ar',str(SR),'-f','f32le','pipe:1'],capture_output=True,check=True)
    return np.frombuffer(p.stdout,dtype='<f4')

def measure(signal,t):
    # 80ms centered around onset; Hann-windowed frequency band energy.
    center=int(t*SR); n=2048
    lo=center-n//2; hi=lo+n
    if lo<0 or hi>len(signal):return None
    x=signal[lo:hi].astype(np.float64)*np.hanning(n)
    power=np.abs(np.fft.rfft(x))**2
    hz=np.fft.rfftfreq(n,1/SR)
    total=power[(hz>=30)&(hz<5000)].sum()
    if total<=1e-12:return None
    bass=power[(hz>=30)&(hz<160)].sum()/total
    mid=power[(hz>=160)&(hz<2000)].sum()/total
    return bass,mid

rows=[]
for j in joins:
    right=float(j['right_start_s']); left=float(j['left_start_s'])
    row={'left_start_s':left,'right_start_s':right,'historic_phase_match':True}
    for side,offset,origin in [('left',left,right-4),('right',right,right)]:
        signal=clip(origin)
        selected=[e for e in events[offset] if origin+.05<=float(e['absolute_event_s'])<=origin+3.95]
        vals=[]
        for e in selected:
            bands=measure(signal,float(e['absolute_event_s'])-origin)
            if bands is not None:vals.append((e['kind'],*bands))
        kick=[x for x in vals if x[0]=='KICK']; other=[x for x in vals if x[0]!='KICK']
        row[side+'_detector_kick_count']=len(kick)
        row[side+'_other_count']=len(other)
        row[side+'_kick_bass_median']=round(float(np.median([x[1] for x in kick])),5) if kick else ''
        row[side+'_other_bass_median']=round(float(np.median([x[1] for x in other])),5) if other else ''
        row[side+'_kick_high_bass_count']=sum(x[1]>=.50 for x in kick)
        row[side+'_other_high_bass_count']=sum(x[1]>=.50 for x in other)
    # Never infer ground truth or authorize warp from spectral shares alone.
    row['independent_audio_measurement']=True
    row['verified_kick_identity']=False
    row['warp_authorized']=False
    rows.append(row)
with open(OUTPUT,'w',newline='') as f:
    w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
print('joins remeasured:',len(rows),'verified kick identities: 0','warp authorized: 0')
for r in rows:print(r['right_start_s'],r['left_kick_bass_median'],r['right_kick_bass_median'])
