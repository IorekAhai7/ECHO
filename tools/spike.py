"""Local acoustic smoke test; synthetic references are NOT human validation."""
import json,time,wave,argparse,resource
from pathlib import Path
import numpy as np
import onnxruntime as ort
ort.disable_telemetry_events()
p=argparse.ArgumentParser();p.add_argument('--output',type=Path);a=p.parse_args()
r=Path(__file__).resolve().parents[1];assets=r/'app/src/main/assets'
v=json.loads((assets/'models/vocab.json').read_text());opts=ort.SessionOptions();opts.intra_op_num_threads=4
start=time.perf_counter();s=ort.InferenceSession(str(assets/'models/model.int8.onnx'),opts,providers=['CPUExecutionProvider']);load=time.perf_counter()-start
results=[]
for e in json.loads((assets/'exercises.json').read_text()):
    with wave.open(str(assets/f'reference/{e["id"]}.wav')) as w:
        sr=w.getframerate();x=np.frombuffer(w.readframes(w.getnframes()),dtype='<i2').astype(np.float32)/32768
    x=np.interp(np.arange(0,len(x),sr/16000),np.arange(len(x)),x).astype(np.float32)
    x=(x-x.mean())/np.sqrt(x.var()+1e-7)
    t=time.perf_counter();logits=s.run(None,{'input_values':x[None,:]})[0][0];elapsed=time.perf_counter()-t
    heard=[];last=-1
    for i in logits.argmax(-1):
        if i!=last and i>3:heard.append(v['id_to_phoneme'][str(i)])
        last=i
    result=dict(id=e['id'],text=e['text'],expected=[p for w in e['words'] for p in w['phones']],heard=heard,seconds=round(elapsed,3),audio_seconds=round(len(x)/16000,3))
    results.append(result);print(json.dumps(result,ensure_ascii=False),flush=True)
report=dict(note='Synthetic Kokoro audio; does not validate learner accuracy. Host CPU, not phone benchmark.',model_load_seconds=load,peak_rss_bytes=resource.getrusage(resource.RUSAGE_SELF).ru_maxrss,results=results)
if a.output:a.output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
