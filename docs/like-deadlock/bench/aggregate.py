# 3회 측정 집계: python3 aggregate.py <bench 폴더> → summary-3runs.tsv (중앙값, 최소, 최대, 회차별 값)
import csv,statistics,sys,os
base=sys.argv[1]
cols=['deadlocks','retry_optimistic','exhausted','count_mismatch','row_lock_waits','latency_p50_ms','latency_p95_ms','round_p50_ms','round_p95_ms']
rows={}
for cond in ('before','after'):
    for r in (1,2,3):
        with open(f'{base}/{cond}/run-{r}/summary.tsv') as f:
            for row in csv.DictReader(f,delimiter='\t'):
                rows.setdefault((row['scenario'],cond),[]).append(row)
def fmt(v,ms): return f'{v:.1f}' if ms else f'{int(v):,}'
out=['scenario\tcondition\tmetric\tmedian\tmin\tmax\trun1\trun2\trun3']
for sc in ('high','mid','none'):
    for cond in ('before','after'):
        rs=rows[(sc,cond)]
        for c in cols:
            vals=[float(x[c]) for x in rs]
            ms=c.endswith('_ms')
            out.append('\t'.join([sc,cond,c,fmt(statistics.median(vals),ms),fmt(min(vals),ms),fmt(max(vals),ms)]+[fmt(v,ms) for v in vals]))
open(f'{base}/summary-3runs.tsv','w').write('\n'.join(out)+'\n')
print('\n'.join(out))
