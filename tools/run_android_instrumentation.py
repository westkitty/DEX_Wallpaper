#!/usr/bin/env python3
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
log=[]
p=subprocess.Popen(['./gradlew','--no-daemon','--console=plain','connectedDebugAndroidTest'],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
for line in p.stdout:
    print(line,end='',flush=True); log.append(line)
    if len(log)>1000: log.pop(0)
code=p.wait()
results=[ET.parse(f).getroot() for f in Path('app/build/outputs/androidTest-results/connected').rglob('*.xml')]
tests=sum(int(r.get('tests',0)) for r in results); failures=sum(int(r.get('failures',0))+int(r.get('errors',0)) for r in results)
message=f'API 35 emulator: {tests} instrumentation tests; {failures} failures/errors. '
if code: message+=''.join(l for l in log if 'error' in l.lower() or 'failed' in l.lower() or l.startswith('e:'))[-5500:]
message=message.replace('%','%25').replace('\n','%0A').replace('\r','%0D')
print(f'::{"error" if code else "notice"} title=Android device verification::{message}')
if not code and tests==0: raise SystemExit('No instrumentation results; cannot claim device verification')
sys.exit(code)
