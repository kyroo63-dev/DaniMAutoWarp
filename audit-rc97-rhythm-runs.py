#!/usr/bin/env python3
"""Independent 24-second clip rhythm-run diagnostic; no production warp authorization.
Replays RC91 events. Uses no ground-truth kick labels. A phase-consistent run is
not proof of kick identity: snares on a regular grid can pass.
"""
import csv, math, sys
from collections import defaultdict

def cluster(events, bpm):
    threshold = 60 / bpm * 0.55
    groups = []
    for e in sorted(events, key=lambda x: (x[0], -x[2])):
        if not groups or e[0] - groups[-1][0][0] >= threshold:
            groups.append([e])
        else:
            groups[-1].append(e)
    return [min(g, key=lambda x: (-x[2], x[0])) for g in groups]

def filter_candidates(events, bpm):
    period = 60 / bpm
    result = []
    for i, (t, bass, spectral) in enumerate(events):
        neighbours = sum(1 for j, (u, _, _) in enumerate(events) if i != j and
                         1 <= round(abs(t-u)/period) <= 2 and
                         abs(abs(t-u)/period - round(abs(t-u)/period)) <= 0.16)
        if bass >= 0.65 and spectral >= 0.60 and neighbours >= 2:
            result.append(t)
    return sorted(result)

def runs(times, bpm, tolerance=0.12, max_skip=2):
    """Separate chains on long gaps or off-grid intervals; no gap interpolation."""
    if not times:
        return []
    period = 60 / bpm
    out, current = [], [times[0]]
    for t in times[1:]:
        dt = t-current[-1]
        beats = round(dt/period)
        if 1 <= beats <= max_skip and abs(dt/period-beats) <= tolerance:
            current.append(t)
        else:
            out.append(current)
            current = [t]
    out.append(current)
    return out

def inspect(times, bpm, duration):
    chains = runs(times,bpm)
    long = [c for c in chains if len(c) >= 4]
    longest = max((len(c) for c in chains), default=0)
    supported = sum(len(c) for c in long)
    # 'supported' measures chain membership, not verified true kicks.
    return dict(candidates=len(times), run_count=len(chains), long_runs=len(long),
                longest_run=longest, run_supported=supported,
                run_fraction=round(supported/len(times),4) if times else 0,
                duration_s=duration,
                diagnostic_stable=(longest >= 8 and supported >= 12 and
                                   supported/len(times) >= 0.65 if times else False))

def audit(events_file,summary_file,output):
    events=defaultdict(list)
    with open(events_file,newline='') as f:
        for row in csv.DictReader(f):
            if row['kind']=='KICK':
                events[int(float(row['offset_s']))].append((float(row['event_s']),float(row['bass_share']),float(row['spectral_score'])))
    rows=[]
    with open(summary_file,newline='') as f:
        for seg in csv.DictReader(f):
            offset=int(float(seg['offset_s'])); bpm=float(seg['bpm'])
            selected=filter_candidates(cluster(events[offset],bpm),bpm)
            result=inspect(selected,bpm,float(seg['duration_s']))
            rows.append(dict(offset_s=offset,bpm=bpm,original_confidence=seg['confidence'],
                             original_automatic_warp=seg['automatic_warp'],**result,
                             warp_authorized_by_rc97='false',
                             note='diagnostic_only_no_ground_truth_or_global_phase'))
    with open(output,'w',newline='') as f:
        writer=csv.DictWriter(f,fieldnames=list(rows[0]))
        writer.writeheader();writer.writerows(rows)
    print('clips',len(rows),'filtered',sum(r['candidates'] for r in rows),
          'diagnostic_stable',sum(r['diagnostic_stable'] for r in rows),
          'production_warp_newly_authorized',0)

if __name__=='__main__': audit(*sys.argv[1:])
