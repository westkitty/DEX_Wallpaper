#!/usr/bin/env python3
"""Fail closed on missing IDs, invented statuses/evidence, and mismatched generated totals."""
import json
import sys
from pathlib import Path
from collections import Counter
ROOT = Path(__file__).resolve().parents[1]
STATUSES = {"DONE_VERIFIED","IMPLEMENTED_UNTESTED","PARTIAL","BLOCKED_PLATFORM","BLOCKED_EXTERNAL","NOT_IMPLEMENTED"}
REQUIRED = {"id","name","subsystem","source_file","ui_location","implementation_status","verification_status","test_reference","permissions","api_constraints","dependencies","limitations"}

def validate(data, root=ROOT):
    errors = []
    rows = data.get("features",[])
    ids = [f.get("id") for f in rows]
    if len(ids) != len(set(ids)): errors.append("Duplicate feature IDs")
    if set(ids) != {f"{n:03}" for n in range(1,277)}: errors.append("IDs must cover exactly 001–276")
    for row in rows:
        fid = row.get("id","?")
        if REQUIRED - row.keys(): errors.append(f"{fid}: Missing required fields {sorted(REQUIRED-row.keys())}")
        if row.get("implementation_status") not in STATUSES: errors.append(f"{fid}: Invalid implementation status")
        for field in ["source_file","test_reference","permissions","api_constraints","dependencies"]:
            if not isinstance(row.get(field),list): errors.append(f"{fid}: {field} must be a list")
        for ref in row.get("source_file",[]) + row.get("test_reference",[]):
            path, _, anchor = ref.partition("#")
            target = root / path
            if not path or Path(path).is_absolute() or ".." in Path(path).parts or not target.is_file(): errors.append(f"{fid}: Missing/unsafe reference {ref}")
            elif anchor and anchor not in target.read_text(): errors.append(f"{fid}: Evidence anchor not found {ref}")
        if row.get("implementation_status") == "DONE_VERIFIED":
            if not row.get("source_file") or not row.get("test_reference") or row.get("verification_status") in (None,"NOT_RUN","DEVICE_PENDING"):
                errors.append(f"{fid}: Verified completion needs implementation and passing evidence")
    totals = dict(Counter(row.get("implementation_status") for row in rows))
    totals = {s:totals.get(s,0) for s in STATUSES}
    if data.get("total") != 276 or data.get("counts") != totals or sum(totals.values()) != 276: errors.append("Totals do not reconcile")
    return errors

if __name__ == "__main__":
    data = json.loads((ROOT / "feature_matrix.json").read_text())
    errors = validate(data)
    human = (ROOT / "FEATURE_MATRIX.md").read_text()
    for s,n in data.get("counts",{}).items():
        if f"**{s}**: {n}" not in human: errors.append(f"Markdown total differs for {s}")
    for row in data.get("features",[]):
        if f"| {row['id']} | {row['name']} | {row['implementation_status']} |" not in human: errors.append(f"Markdown row differs for {row['id']}")
    if errors: print("\n".join(errors),file=sys.stderr); sys.exit(1)
    print("PASS: 276 unique requirements; fields, paths, evidence, and totals valid.")
