#!/usr/bin/env python3
"""Expose actual Gradle failures as check annotations when sandbox log redirects are blocked."""
import hashlib
import os
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

def annotation(level,title,message):
    message=message.replace('%','%25').replace('\r','%0D').replace('\n','%0A')
    print(f'::{level} title={title}::{message}',flush=True)

mode=sys.argv[1] if len(sys.argv)>1 else 'build'
if mode=='sdk':
    sdk=Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','/usr/local/lib/android/sdk')))
    required=[sdk/'platforms/android-35/android.jar',sdk/'build-tools/35.0.0/aapt2']
    missing=[str(p) for p in required if not p.is_file()]
    if missing:
        annotation('error','Android SDK preflight','Missing hosted SDK files: '+', '.join(missing))
        sys.exit(1)
    annotation('notice','Android SDK verified','Hosted SDK platform 35 and build-tools 35.0.0 exist; no redundant command-line tools download.')
    sys.exit(0)
log=[]
command=['./gradlew','--no-daemon','--console=plain','testDebugUnitTest','lintDebug','assembleDebug']
process=subprocess.Popen(command,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
for line in process.stdout:
    print(line,end='',flush=True)
    log.append(line)
    if len(log)>1000: log.pop(0)
code=process.wait()
Path('app/build/reports').mkdir(parents=True,exist_ok=True)
Path('app/build/reports/ci-tail.log').write_text(''.join(log))
results=[ET.parse(p).getroot() for p in Path('app/build/test-results/testDebugUnitTest').glob('TEST-*.xml')]
tests=sum(int(r.get('tests',0)) for r in results)
failures=sum(int(r.get('failures',0))+int(r.get('errors',0)) for r in results)
if results:
    annotation('notice','JVM test results',f'{tests} tests; {failures} failures/errors (independent of later lint/APK results).')
if code:
    indices=set()
    for i,line in enumerate(log):
        if line.startswith('e:') or 'error:' in line.lower() or 'What went wrong' in line or 'FAILURE' in line or 'failed' in line.lower():
            indices.update(range(max(0,i-1),min(len(log),i+8)))
    text=''.join(log[i] for i in sorted(indices))[-6000:] or ''.join(log[-60:])[-6000:]
    annotation('error','Android verification failed',text)
    sys.exit(code)
apk=Path('app/build/outputs/apk/debug/app-debug.apk')
annotation('notice','Android verification passed',f'{tests} JVM tests; {failures} failures/errors; lintDebug and assembleDebug succeeded; APK SHA-256 {hashlib.sha256(apk.read_bytes()).hexdigest()}')
