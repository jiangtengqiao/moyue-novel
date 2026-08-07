import json, urllib.request, os, tarfile, io

REGISTRY = 'https://registry.npmmirror.com'
NM_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'nm')
installed = set()

def get_info(name):
    with urllib.request.urlopen(REGISTRY + '/' + name, timeout=15) as r:
        return json.loads(r.read())

def best_version(info, spec):
    spec = spec.lstrip('^~>=< ')
    parts = spec.split('.')
    maj = int(parts[0])
    minv = int(parts[1]) if len(parts) > 1 else 0
    pat = int(parts[2]) if len(parts) > 2 else 0
    best = None
    for ver in info.get('versions', {}):
        try:
            vp = ver.split('.')
            v0 = int(vp[0])
            v1 = int(vp[1]) if len(vp) > 1 else 0
            v2 = int(vp[2]) if len(vp) > 2 else 0
            if v0 != maj:
                continue
            if v0 == 0 and v1 != minv:
                continue
            if v0 > 0 and v1 < minv:
                continue
            if v0 > 0 and v1 == minv and v2 < pat:
                continue
            if best is None:
                best = ver
            else:
                bp = best.split('.')
                if int(vp[1]) > int(bp[1]) or (int(vp[1]) == int(bp[1]) and int(vp[2]) > int(bp[2])):
                    best = ver
        except:
            continue
    return best or info.get('dist-tags', {}).get('latest')

def dl(name, spec):
    info = get_info(name)
    ver = best_version(info, spec)
    key = name + '@' + ver
    if key in installed:
        return
    installed.add(key)
    vinfo = info['versions'][ver]
    tb = vinfo['dist']['tarball']
    print('  ' + name + '@' + ver)
    with urllib.request.urlopen(tb, timeout=30) as r:
        data = r.read()
    tf = tarfile.open(fileobj=io.BytesIO(data), mode='r:gz')
    d = os.path.join(NM_DIR, name)
    os.makedirs(d, exist_ok=True)
    for m in tf.getmembers():
        if m.name.startswith('package/'):
            rel = m.name[9:]
            if not rel:
                continue
            tgt = os.path.join(d, rel)
            if m.isdir():
                os.makedirs(tgt, exist_ok=True)
            elif m.isfile():
                pd = os.path.dirname(tgt)
                if pd:
                    os.makedirs(pd, exist_ok=True)
                with open(tgt, 'wb') as f:
                    f.write(tf.extractfile(m).read())
    for dn, dv in vinfo.get('dependencies', {}).items():
        dl(dn, dv)

print('重新安装 express@4.18.2...')
dl('express', '4.18.2')
print('安装 body-parser...')
dl('body-parser', '1.20.2')
print('完成! 共 ' + str(len(installed)) + ' 个包')
