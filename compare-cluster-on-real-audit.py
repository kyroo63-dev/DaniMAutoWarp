#!/usr/bin/env python3
"""Re-evaluate RC91 real-detector candidates with legacy and anchored clustering.
Uses real recorded candidate times/spectral scores; does not re-run the detector.
"""
import csv,sys,math
from collections import defaultdict

def group(events,bpm,anchored):
    threshold=60/bpm*.55
    clusters=[]
    for event in sorted(events,key=lambda e:(e[0],-e[1])):
        if not clusters or event[0]-(clusters[-1][0][0] if anchored else clusters[-1][-1][0])>=threshold:
            clusters.append([event])
        else: clusters[-1].append(event)
    return [min(c,key=lambda e:(-e[1],e[0])) for c in clusters]

def main(events_file,summary_file,out_file):
    with open(summary_file,newline='') as f: rows=list(csv.DictReader(f))
    by_offset=defaultdict(list)
    with open(events_file,newline='') as f:
        for e in csv.DictReader(f):
            if e['kind']=='KICK':
                by_offset[int(e['offset_s'])].append((float(e['event_s']),float(e['spectral_score'])))
    with open(out_file,'w',newline='') as f:
        writer=csv.writer(f)
        writer.writerow(['offset_s','duration_s','bpm_reference','raw_kick_candidates','legacy_retained','anchored_retained','additional_retained','legacy_median_spacing_s','anchored_median_spacing_s','comparison_note'])
        for row in rows:
            offset=int(row['offset_s']); bpm=float(row['bpm']); events=by_offset[offset]
            legacy=group(events,bpm,False); anchored=group(events,bpm,True)
            def median_spacing(items):
                d=sorted(b[0]-a[0] for a,b in zip(items,items[1:]));return f'{d[len(d)//2]:.5f}' if d else ''
            writer.writerow([offset,row['duration_s'],row['bpm'],len(events),len(legacy),len(anchored),len(anchored)-len(legacy),median_spacing(legacy),median_spacing(anchored),'diagnostic_only_no_ground_truth'])
    print('segments',len(rows),'candidates',sum(len(by_offset[int(r['offset_s'])]) for r in rows))
    print('legacy_retained',sum(len(group(by_offset[int(r['offset_s'])],float(r['bpm']),False)) for r in rows))
    print('anchored_retained',sum(len(group(by_offset[int(r['offset_s'])],float(r['bpm']),True)) for r in rows))
if __name__=='__main__':main(*sys.argv[1:])
