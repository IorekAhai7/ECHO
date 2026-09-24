"""Optional build-time reference tooling. All inputs validated against hashes."""
import pathlib,json,urllib.request,hashlib,os
root=pathlib.Path(__file__).resolve().parents[1]
dest=root/'work/tts';dest.mkdir(parents=True,exist_ok=True)
for name,spec in json.loads((root/'tools/tts-manifest.json').read_text()).items():
    file=dest/name
    def digest(p):
        with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
    if file.exists() and digest(file)==spec['sha256']:continue
    tmp=file.with_suffix('.part')
    try:
        print('Downloading',name,flush=True)
        with urllib.request.urlopen(spec['url'],timeout=60) as response,tmp.open('wb') as out:
            while chunk:=response.read(1024*1024):out.write(chunk)
        assert digest(tmp)==spec['sha256'],f'Upstream changed: {name}; review, do not bypass integrity check'
        os.replace(tmp,file)
    finally:tmp.unlink(missing_ok=True)
