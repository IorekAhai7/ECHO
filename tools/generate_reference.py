"""Build-time local Kokoro synthesis. No TTS engine or network in the app."""
import argparse, json, wave
from pathlib import Path
import numpy as np
import onnxruntime as ort
ort.disable_telemetry_events()
p=argparse.ArgumentParser();p.add_argument('--tts-dir',type=Path,required=True);a=p.parse_args()
root=Path(__file__).resolve().parents[1]
vocab=json.loads((a.tts_dir/'tokens.json').read_text())['vocab']
voice=np.fromfile(a.tts_dir/'voice.bin',dtype=np.float32).reshape(-1,1,256)
opt=ort.SessionOptions();opt.intra_op_num_threads=4
s=ort.InferenceSession(str(a.tts_dir/'model.onnx'),opt,providers=['CPUExecutionProvider'])
for e in json.loads((root/'app/src/main/assets/exercises.json').read_text()):
    tokens=[vocab[c] for c in e['tts']]
    audio=s.run(None,{'input_ids':np.array([[0,*tokens,0]],dtype=np.int64),'style':voice[len(tokens)],'speed':np.ones(1,dtype=np.float32)})[0].reshape(-1)
    assert np.isfinite(audio).all()
    with wave.open(str(root/f'app/src/main/assets/reference/{e["id"]}.wav'),'wb') as w:
        w.setnchannels(1);w.setsampwidth(2);w.setframerate(24000);w.writeframes((np.clip(audio,-1,1)*32767).astype('<i2').tobytes())
    print(e['id'],e['text'],round(len(audio)/24000,2),flush=True)
