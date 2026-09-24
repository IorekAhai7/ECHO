#!/usr/bin/env python3
"""Download once at BUILD time; verify pinned weights. Never run on the phone."""
import hashlib, pathlib, urllib.request, os
ROOT=pathlib.Path(__file__).resolve().parents[1]
REV='1bc86aec48933fdced56df9e16bc132152d208e8'
REPO='iwillsolvehardestproblem/wav2vec2-xlsr-53-espeak-cv-ft-onnx'
FILES={'model.int8.onnx':'547beefce34b32fd2b469717ff87294cfe650f1d5d39591368dcc912eba2d7df','vocab.json':'3768692b2b3c85b12c0608181dface0544d99e5ead0c76d22c94e8bf97251164'}
def digest(path):
    with path.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def main():
    directory=ROOT/'app/src/main/assets/models';directory.mkdir(parents=True,exist_ok=True)
    for name,sha in FILES.items():
        dest=directory/name
        if dest.exists() and digest(dest)==sha:print(name,'verified');continue
        tmp=dest.with_suffix(dest.suffix+'.part')
        print('Downloading',name,flush=True)
        try:
            with urllib.request.urlopen(f'https://huggingface.co/{REPO}/resolve/{REV}/{name}',timeout=60) as response, tmp.open('wb') as out:
                while block:=response.read(1024*1024):out.write(block)
            if digest(tmp)!=sha:raise RuntimeError(f'Checksum mismatch: {name}')
            os.replace(tmp,dest)
        finally:
            tmp.unlink(missing_ok=True)
        print(name,'verified')
if __name__=='__main__':main()
