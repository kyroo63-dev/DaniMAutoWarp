#!/usr/bin/env python3
import csv,tempfile,pathlib,importlib.util
spec=importlib.util.spec_from_file_location('review','review-rc103-attacks.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
with tempfile.TemporaryDirectory() as tmp:
    p=pathlib.Path(tmp)/'queue.csv'
    n=m.prepare('rc102-fusion-events.csv',p,per_group=4)
    assert n>0
    with p.open(newline='') as f:rows=list(csv.DictReader(f))
    assert len(rows)==n and all(not r['human_label'] for r in rows)
    assert all(not r['reviewer'] for r in rows)
    assert len({r['review_id'] for r in rows})==n
    r=m.evaluate(p);assert r['verified']==0 and r['precision'] is None and r['recall'] is None
    rows[0]['human_label']='kick';rows[0]['reviewer']='human-test'
    rows[1]['human_label']='snare';rows[1]['reviewer']='human-test'
    with p.open('w',newline='') as f:
        w=csv.DictWriter(f,fieldnames=m.FIELDS);w.writeheader();w.writerows(rows)
    assert m.evaluate(p)['verified']==2
    rows[0]['reviewer']=''
    with p.open('w',newline='') as f:
        w=csv.DictWriter(f,fieldnames=m.FIELDS);w.writeheader();w.writerows(rows)
    try:m.evaluate(p);assert False
    except ValueError:pass
print('RC103: 7 review-queue assertions passed')
