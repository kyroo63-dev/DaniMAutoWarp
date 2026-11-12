#!/usr/bin/env python3
"""Diagnostic fusion on fresh FLAC features. Detector labels are NOT truth."""
import csv,sys,subprocess
from collections import defaultdict
import numpy as np
AUDIO,JOINS,EVENTS,OUT,TIMELINE=sys.argv[1:]
with open(JOINS,newline='') as f: joins=[r for r in csv.DictReader(f) if r['diagnostic_match'].lower()=='true']
with open(EVENTS,newline='') as f: all_events=list(csv.DictReader(f))
by_offset=defaultdict(list)
for e in all_events:by_offset[float(e['offset_s'])].append(e)
SR=22050

def extract(start):
 p=subprocess.run(['ffmpeg','-nostdin','-v','error','-ss',str(start),'-i',AUDIO,'-t','8','-ac','1','-ar',str(SR),'-f','f32le','pipe:1'],capture_output=True,check=True)
 return np.frombuffer(p.stdout,dtype='<f4')
def measure(audio,t):
 c=round(t*SR);n=4096;a=c-1024
 if a<0 or a+n>len(audio) or c-round(.045*SR)<0 or c+round(.14*SR)>len(audio):return None
 x=audio[a:a+n].astype(np.float64)*np.hanning(n);p=np.abs(np.fft.rfft(x))**2;hz=np.fft.rfftfreq(n,1/SR)
 mask=(hz>=30)&(hz<5000);total=p[mask].sum()
 if total<=1e-12:return None
 bass=float(p[(hz>=30)&(hz<160)].sum()/total)
 centroid=float((hz[mask]*p[mask]).sum()/total)
 pre=np.mean(audio[c-round(.045*SR):c].astype(np.float64)**2)
 post=np.mean(audio[c:c+round(.14*SR)].astype(np.float64)**2)
 attack=float(10*np.log10((post+1e-10)/(pre+1e-10)))
 return bass,centroid,attack
rows=[]
for j in joins:
 left=float(j['left_start_s']);right=float(j['right_start_s'])
 for side,offset,start in [('left',left,right-8),('right',right,right)]:
  audio=extract(start);candidates=[]
  for e in by_offset[offset]:
   absolute=float(e['absolute_event_s']);t=absolute-start
   if .20<=t<=7.75:
    features=measure(audio,t)
    if features:candidates.append((absolute,e['kind'],features))
  # Independent of historical rhythm_score; uses observed onset spacing and segment BPM.
  # Do not count simultaneous events (zero-beat distance) as rhythmic neighbours.
  # BPM from RC91 window estimates is provided as an optional fifth CLI arg.
  for absolute,kind,(bass,centroid,attack) in candidates:
   # Local periodicity: require at least two distinct 1-2 beat neighbours using BPM supplied from window CSV.
   rows.append(dict(join_s=right,side=side,window_start_s=start,event_s=round(absolute,6),historical_label=kind,
     bass_share=round(bass,6),centroid_hz=round(centroid,3),attack_db=round(attack,4)))
# Determine local BPM from original timeline audit, supplied in fifth argument.
if len(sys.argv)!=6:raise SystemExit('Usage: audit-rc102-fusion.py AUDIO JOINS EVENTS OUT TIMELINE')
with open(TIMELINE,newline='') as f: bpms={float(r['offset_s']):float(r['bpm']) for r in csv.DictReader(f)}
windows=defaultdict(list)
for r in rows:windows[(r['join_s'],r['side'])].append(r)
for key,items in windows.items():
 # Historical timeline offsets are the left or right 24-second segment, not the 8s analysis start.
 join,side=key;offset=join-24 if side=='left' else join
 period=60/bpms[offset]
 for r in items:
  support=sum(1 for q in items if q is not r and q['bass_share']>=.65 and q['centroid_hz']<=350 and q['attack_db']>=-3 and 1<=round(abs(q['event_s']-r['event_s'])/period)<=2 and abs(abs(q['event_s']-r['event_s'])/period-round(abs(q['event_s']-r['event_s'])/period))<=.12)
  r['rhythm_neighbours']=support
  r['spectral_candidate']=r['bass_share']>=.65 and r['centroid_hz']<=350 and r['attack_db']>=-3
  r['fusion_candidate']=r['spectral_candidate'] and support>=2
  r['verified_kick_identity']=False;r['warp_authorized']=False
fields=list(rows[0])
with open(OUT,'w',newline='') as f:
 w=csv.DictWriter(f,fieldnames=fields);w.writeheader();w.writerows(rows)
print('events',len(rows),'fusion candidates',sum(r['fusion_candidate'] for r in rows),'historical OTHER fusion candidates',sum(r['fusion_candidate'] and r['historical_label']!='KICK' for r in rows),'windows',len(windows))
