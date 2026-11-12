#!/usr/bin/env python3
"""Validate RC102 audit invariants and historical-label ambiguity."""
import csv,sys,math
p=sys.argv[1]
with open(p,newline='') as f: rows=list(csv.DictReader(f))
assert len(rows)==778
assert len({(r['join_s'],r['side']) for r in rows})==8
assert all(r['verified_kick_identity']=='False' and r['warp_authorized']=='False' for r in rows)
assert all(0<=float(r['bass_share'])<=1 and math.isfinite(float(r['centroid_hz'])) and math.isfinite(float(r['attack_db'])) for r in rows)
assert all(int(r['rhythm_neighbours'])>=0 for r in rows)
assert all(r['fusion_candidate']!='True' or (r['spectral_candidate']=='True' and int(r['rhythm_neighbours'])>=2) for r in rows)
assert sum(r['fusion_candidate']=='True' for r in rows)==503
assert sum(r['fusion_candidate']=='True' and r['historical_label']!='KICK' for r in rows)==284
print('RC102: 7 assertions passed; diagnostic only')
