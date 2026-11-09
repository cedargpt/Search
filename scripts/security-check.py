"""Fail publication on obvious credentials, local identifiers or capability drift.

This is a release guard, not a claim of a complete security audit. Never prints
matching secret contents. Optional --staged checks exactly the Git index too.
"""
import argparse
import hashlib
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET
from release_files import ROOT, collect

def check():
    files = collect()
    # Search 1.2.0 no longer bundles the CJK font or platform artwork (see THIRD_PARTY_NOTICES.md),
    # so the upstream binary hash pins are dropped here.
    license_text = (ROOT/'LICENSE').read_text(encoding='utf-8')
    assert 'GNU AFFERO GENERAL PUBLIC LICENSE' in license_text and '13. Remote Network Interaction' in license_text
    assert (ROOT/'app/src/main/res/raw/agpl_3.txt').read_text(encoding='utf-8') == license_text
    patterns = {
        'private key': r'-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----',
        'GitHub token': r'\b(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{30,})\b',
        'AWS access key': r'\b(?:AKIA|ASIA)[A-Z0-9]{16}\b',
        'Slack token': r'\bxox[baprs]-[A-Za-z0-9-]{20,}\b',
        'personal path': r'(?i)\b[A-Z]:[\\/](?:Users[\\/][A-Za-z0-9_.-]+|code[\\/][A-Za-z0-9_.-]+)',
        'WeChat account path': r'\bwxid_[A-Za-z0-9_]{5,}',
        'private mail': r'\b[0-9]{5,12}@qq\.com\b',
    }
    failures = []
    for name in files:
        if Path(name).suffix in {'.png', '.jar', '.otf'}:
            continue
        source = (ROOT/name).read_text(encoding='utf-8-sig')
        for label, pattern in patterns.items():
            for match in re.finditer(pattern, source):
                line = source.count('\n', 0, match.start())+1
                failures.append(f'{name}:{line}: {label}')
    android = '{http://schemas.android.com/apk/res/android}'
    manifest = ET.parse(ROOT/'app/src/main/AndroidManifest.xml').getroot()
    assert not manifest.findall('uses-permission'), 'Review new runtime permissions before publishing'
    expected_packages = {
        'com.douban.frodo', 'com.taobao.taobao', 'com.taobao.idlefish', 'com.tencent.mm',
        'tv.danmaku.bili', 'com.xunmeng.pinduoduo', 'com.xingin.xhs', 'com.sankuai.meituan',
        'com.autonavi.minimap', 'com.ss.android.ugc.aweme', 'com.zhihu.android',
        'com.dianping.v1', 'com.netease.cloudmusic', 'com.coolapk.market',
        'com.xueqiu.android', 'com.tencent.weread',
    }
    queries = manifest.find('queries')
    assert queries is not None and {p.get(android+'name') for p in queries.findall('package')} == expected_packages, 'Only the sixteen supported packages may be queried'
    assert not queries.findall('intent') and not queries.findall('provider'), 'No broad app discovery'

    app = manifest.find('application')
    assert app.get(android+'allowBackup') == 'false'
    assert app.get(android+'fullBackupContent') == 'false'
    sheet = app.find("activity[@"+android+"name='.WidgetSearchActivity']")
    assert sheet is not None and sheet.get(android+'exported') == 'false'
    adapter = app.find("service[@"+android+"name='.WidgetAppsService']")
    assert adapter is not None and adapter.get(android+'permission') == 'android.permission.BIND_REMOTEVIEWS'
    assert adapter.get(android+'exported') == 'false'
    for receiver in app.findall('receiver'):
        assert receiver.get(android+'exported') == 'false'
    service = app.find("service[@"+android+"name='.WechatSearchService']")
    assert service is not None and service.get(android+'permission') == 'android.permission.BIND_ACCESSIBILITY_SERVICE'
    config = ET.parse(ROOT/'app/src/main/res/xml/wechat_accessibility.xml').getroot()
    assert config.get(android+'canRetrieveWindowContent') == 'false'
    assert config.get(android+'canPerformGestures') == 'true'
    implementation = (ROOT/'app/src/main/java/org/nofeed/android/WechatSearchService.java').read_text(encoding='utf-8-sig')
    assert not re.search(r'getRootInActiveWindow|getWindows\(|getPrimaryClip\(', implementation)
    if failures:
        raise SystemExit('Publication check failed (values redacted):\n'+'\n'.join(failures))
    return files

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--staged', action='store_true')
    args = parser.parse_args()
    files = check()
    if args.staged:
        staged = subprocess.check_output(['git', 'ls-files', '-z'], cwd=ROOT).decode('utf-8').split('\0')
        staged = {name for name in staged if name}
        assert staged == set(files), 'Git index differs from reviewed release allowlist'
        for name in staged:
            blob = subprocess.check_output(['git', 'show', ':'+name], cwd=ROOT)
            local = (ROOT/name).read_bytes()
            if Path(name).suffix not in {'.png', '.jar', '.otf'}:
                blob = blob.replace(b'\r\n', b'\n'); local = local.replace(b'\r\n', b'\n')
            assert blob == local, 'Index differs from reviewed file: '+name
    print(f'PASS: {len(files)} reviewed source files; credential/path scan and Android capability guards')
