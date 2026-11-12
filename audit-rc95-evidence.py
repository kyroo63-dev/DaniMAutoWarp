#!/usr/bin/env python3
"""Exploratory replication of Kotlin gate on RC91 event features; not a ground-truth accuracy test."""
import csv, math, sys
from collections import defaultdict
from pathlib import Path
source=Path(sys.argv[1]); summary=Path(sys.argv[2]); dest=Path(sys.argv[3])
with summary.open() as f: bpm={float(r['offset_s']):float(r['bpm']) for r in csv.DictReader(f)}
groups=defaultdict(list)
with source.open() as f:
 for r in csv.DictReader(f):
  if r['kind']=='KICK':
   groups[float(r['offset_s'])].append((float(r['event_s']),float(r['bass_share']),float(r['spectral_score'])))
with dest.open('w',newline='') as f:
 w=csv.writer(f);w.writerow(['offset_s','reference_bpm','kick_events','spectral_and_bass_pass','periodic_neighbour_pass','combined_gate_pass','note'])
 for offset in sorted(bpm):
  points=sorted(groups[offset]); period=60/bpm[offset]; spectral=periodic=combined=0
  for i,(t,b,s) in enumerate(points):
   neighbours=sum(1 for j,(u,_,__) in enumerate(points) if j!=i and 1<=round(abs(t-u)/period)<=2 and abs(abs(t-u)/period-round(abs(t-u)/period))<=.16)
   sp=b>=.65 and s>=.60; pe=neighbours>=2
   spectral+=sp;periodic+=pe;combined+=sp and pe
  w.writerow([int(offset),f'{bpm[offset]:.5f}',len(points),spectral,periodic,combined,'diagnostic_only_no_kick_ground_truth'])
