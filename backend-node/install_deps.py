import json, urllib.request, os, tarfile, io, shutil

REGISTRY = 'https://registry.npmmirror.com'
MODULES_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'nm')

os.makedirs(MODULES_DIR, exist_ok=True)

installed = set()

def get_pkg_info(name):
    url = REGISTRY + '/' + name
    try:
        with urllib.request.urlopen(url, timeout=15) as r:
            return json.loads(r.read())
    except Exception as e:
        print('  获取 ' + name + ' 失败: ' + str(e))
        return None

def resolve_version(info, version_spec):
    versions = sorted(info.get('versions', {}).keys())
    if version_spec == 'latest':
        return info.get('dist-tags', {}).get('latest')
    v = version_spec.lstrip('^~>=< ')
    parts = v.split('.')
    try:
        major = int(parts[0])
        minor = int(parts[1]) if len(parts) > 1 else 0
    except ValueError:
        return info.get('dist-tags', {}).get('latest')
    for ver in reversed(versions):
        try:
            vparts = ver.split('.')
            vmaj = int(vparts[0])
            vmin = int(vparts[1]) if len(vparts) > 1 else 0
            if major == 0:
                if vmaj == 0 and vmin == minor:
                    return ver
            else:
                if vmaj == major:
                    return ver
        except:
            continue
    return info.get('dist-tags', {}).get('latest')

def download_and_extract(name, version_spec):
    info = get_pkg_info(name)
    if not info:
        return
    version = resolve_version(info, version_spec)
    if not version:
        print('  无法解析 ' + name + '@' + version_spec)
        return
    key = name + '@' + version
    if key in installed:
        return
    installed.add(key)
    vinfo = info['versions'].get(version)
    if not vinfo:
        version = info.get('dist-tags', {}).get('latest')
        vinfo = info['versions'].get(version)
        if not vinfo:
            return
    tarball = vinfo['dist']['tarball']
    print('  下载 ' + name + '@' + version)
    try:
        with urllib.request.urlopen(tarball, timeout=30) as r:
            data = r.read()
        tf = tarfile.open(fileobj=io.BytesIO(data), mode='r:gz')
        pkg_dir = os.path.join(MODULES_DIR, name)
        os.makedirs(pkg_dir, exist_ok=True)
        for member in tf.getmembers():
            if member.name.startswith('package/'):
                rel = member.name[len('package/'):]
                if not rel:
                    continue
                target = os.path.join(pkg_dir, rel)
                if member.isdir():
                    os.makedirs(target, exist_ok=True)
                elif member.isfile():
                    d = os.path.dirname(target)
                    if d:
                        os.makedirs(d, exist_ok=True)
                    with open(target, 'wb') as f:
                        f.write(tf.extractfile(member).read())
        print('  OK ' + name + '@' + version)
    except Exception as e:
        print('  失败 ' + name + ': ' + str(e))
        return
    deps = vinfo.get('dependencies', {})
    for dep_name, dep_ver in deps.items():
        download_and_extract(dep_name, dep_ver)

packages = {
    'express': '^4.18.2',
    'pg': '^8.11.3',
    'jsonwebtoken': '^9.0.0',
    'bcryptjs': '^2.4.3',
    'multer': '^1.4.5-lts.1',
    'cors': '^2.8.5',
    'adm-zip': '^0.5.10',
    'iconv-lite': '^0.6.3',
}

for pkg, ver in packages.items():
    print('处理 ' + pkg + '@' + ver + '...')
    download_and_extract(pkg, ver)

print('完成! 共安装 ' + str(len(installed)) + ' 个包')
