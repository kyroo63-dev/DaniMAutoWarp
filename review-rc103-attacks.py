#!/usr/bin/env python3
"""RC103 human review queue; historical predictions are NEVER ground truth."""
import argparse,csv,collections,math,pathlib,subprocess,sys
FIELDS=['review_id','join_s','side','event_s','historical_label','fusion_candidate','bass_share','centroid_hz','attack_db','rhythm_neighbours','audio_clip','human_label','reviewer','review_notes']
LABELS={'kick','snare','other','uncertain'}
def prepare(source,output,clips=None,audio=None,per_group=4):
    with open(source,newline='') as f: rows=list(csv.DictReader(f))
    assert all(math.isfinite(float(r['event_s'])) for r in rows)
    groups=collections.defaultdict(list)
    for r in rows: groups[(r['join_s'],r['side'],r['fusion_candidate']=='True')].append(r)
    selected=[]
    for key in sorted(groups):
        pool=sorted(groups[key],key=lambda r:float(r['event_s']))
        # Deterministic evenly-spaced sampling, independent of historical label.
        indices=[min(len(pool)-1,int((i+0.5)*len(pool)/min(per_group,len(pool)))) for i in range(min(per_group,len(pool)))]
        selected.extend(pool[i] for i in indices)
    selected.sort(key=lambda r:(float(r['join_s']),r['side'],float(r['event_s'])))
    if clips is not None:
        if audio is None: raise ValueError('Audio source required for clip generation')
        clips=pathlib.Path(clips);clips.mkdir(parents=True,exist_ok=True)
    with open(output,'w',newline='') as f:
        w=csv.DictWriter(f,fieldnames=FIELDS);w.writeheader()
        for i,r in enumerate(selected,1):
            clip=f'attack_{i:03d}.wav' if clips is not None else ''
            if clips is not None:
                # 0.30 s pre-onset, 0.55 s post-onset; mono PCM for easy playback.
                start=max(0,float(r['event_s'])-0.30)
                subprocess.run(['ffmpeg','-nostdin','-hide_banner','-loglevel','error','-y','-ss',str(start),'-i',str(audio),'-t','0.85','-vn','-ac','1','-ar','16000','-c:a','pcm_s16le',str(clips/clip)],check=True)
            w.writerow({**{k:r.get(k,'') for k in FIELDS},'review_id':f'RC103-{i:03d}','audio_clip':clip,'human_label':'','reviewer':'','review_notes':''})
    return len(selected)
def evaluate(path):
    with open(path,newline='') as f:rows=list(csv.DictReader(f))
    if not rows or not set(FIELDS).issubset(rows[0]):raise ValueError('Invalid annotation CSV')
    ids=[r['review_id'] for r in rows]
    if len(ids)!=len(set(ids)):raise ValueError('Duplicate review IDs')
    for r in rows:
        label=r['human_label'].strip().lower()
        if label and label not in LABELS:raise ValueError('Invalid human label: '+label)
        if label and not r['reviewer'].strip():raise ValueError('Reviewer required for annotated row')
    verified=[r for r in rows if r['human_label'].strip().lower() in LABELS-{'uncertain'}]
    uncertain=sum(r['human_label'].strip().lower()=='uncertain' for r in rows)
    tp=sum(r['fusion_candidate']=='True' and r['human_label'].strip().lower()=='kick' for r in verified)
    fp=sum(r['fusion_candidate']=='True' and r['human_label'].strip().lower()!='kick' for r in verified)
    fn=sum(r['fusion_candidate']!='True' and r['human_label'].strip().lower()=='kick' for r in verified)
    tn=sum(r['fusion_candidate']!='True' and r['human_label'].strip().lower()!='kick' for r in verified)
    return dict(total=len(rows),verified=len(verified),uncertain=uncertain,unreviewed=len(rows)-len(verified)-uncertain,tp=tp,fp=fp,fn=fn,tn=tn,precision=(tp/(tp+fp) if tp+fp else None),recall=(tp/(tp+fn) if tp+fn else None))
def main():
    p=argparse.ArgumentParser();sub=p.add_subparsers(dest='cmd',required=True)
    q=sub.add_parser('prepare');q.add_argument('source');q.add_argument('output');q.add_argument('--audio');q.add_argument('--clips');q.add_argument('--per-group',type=int,default=4)
    e=sub.add_parser('evaluate');e.add_argument('annotations')
    a=p.parse_args()
    if a.cmd=='prepare':print('Review queue:',prepare(a.source,a.output,a.clips,a.audio,a.per_group),'unlabeled examples')
    else:
        result=evaluate(a.annotations)
        for k,v in result.items():print(f'{k}: {v if v is not None else "N/A"}')
        print('Diagnostic evaluation only; stratified review sample is not an unbiased population estimate.')
if __name__=='__main__':main()
