#!/usr/bin/env python3
"""Summarize independent detector windows; never stitch or authorize a warp."""
import csv, sys
from pathlib import Path
if len(sys.argv)!=4:
    raise SystemExit('usage: summarize-full-timeline.py audit.csv duration_seconds output.csv')
rows=list(csv.DictReader(open(sys.argv[1],newline='')))
duration=float(sys.argv[2]); out=Path(sys.argv[3]); out.parent.mkdir(parents=True,exist_ok=True)
with out.open('w',newline='') as f:
    w=csv.writer(f)
    w.writerow(['start_s','end_s','bpm','detector_confidence','kick_candidates','grouped_candidates','supported_windows','adjacent_comparisons','window_stable','automatic_warp_in_isolated_clip','absolute_coverage_warning'])
    for r in rows:
        start=float(r['offset_s']); end=min(duration,start+float(r['duration_s']))
        w.writerow([start,round(end,3),r['bpm'],r['confidence'],r['kick_candidates'],r['cluster_retained'],r['window_supported'],r['window_adjacent'],r['window_stable'],r['automatic_warp'],'independent_clips_not_continuous'])
intervals=sorted((float(r['offset_s']),min(duration,float(r['offset_s'])+float(r['duration_s']))) for r in rows)
merged=[]
for start,end in intervals:
    if merged and start<=merged[-1][1]+1e-6:
        merged[-1]=(merged[-1][0],max(merged[-1][1],end))
    else:
        merged.append((start,end))
covered=sum(end-start for start,end in merged)
gaps=[]
position=0.0
for start,end in merged:
    if start>position+1e-4:gaps.append((position,start))
    position=max(position,end)
if duration>position+1e-4:gaps.append((position,duration))
print(f'{len(rows)} clips; {covered:.2f}/{duration:.2f}s analyzed ({100*covered/duration:.1f}%), gaps={gaps}; no full-track warp validation')
