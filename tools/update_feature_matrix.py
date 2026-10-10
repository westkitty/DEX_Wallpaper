#!/usr/bin/env python3
"""Deterministic inventory; evidence updates are explicit, never inferred from compilation."""
import json
from collections import Counter
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
STATUS = ["DONE_VERIFIED", "IMPLEMENTED_UNTESTED", "PARTIAL", "BLOCKED_PLATFORM", "BLOCKED_EXTERNAL", "NOT_IMPLEMENTED"]
GROUPS = [(36,"Control & rotation"),(74,"Library & content"),(102,"Editing & composition"),(141,"Filters, effects & motion"),
          (160,"Generative & animated content"),(194,"Programmable environment"),(208,"Android automation"),(236,"Reliability & privacy"),
          (244,"Intelligent control"),(252,"Artwork intelligence"),(260,"Tablet experience"),(268,"Operational hardening"),(276,"Source trust & extensibility")]
names = (ROOT / "tools/feature_names.txt").read_text().splitlines()
assert len(names) == 276, f"Expected 276 names; got {len(names)}"
overrides_file = ROOT / "tools/feature_overrides.json"
overrides = json.loads(overrides_file.read_text()) if overrides_file.exists() else {}
features = []
for number, name in enumerate(names,1):
    fid = f"{number:03}"
    row = dict(id=fid,name=name,subsystem=next(n for end,n in GROUPS if number <= end), source_file=[],ui_location="Not exposed",
               implementation_status="NOT_IMPLEMENTED",verification_status="NOT_RUN",test_reference=[],permissions=[],api_constraints=[],dependencies=[],
               limitations="Requirement retained; implementation and acceptance verification remain outstanding.")
    row.update(overrides.get(fid,{}))
    features.append(row)
counts = dict(Counter(f["implementation_status"] for f in features))
for status in STATUS: counts.setdefault(status,0)
result = dict(schema_version=1, total=276, counts=counts, features=features)
(ROOT / "feature_matrix.json").write_text(json.dumps(result,indent=2,ensure_ascii=False)+"\n")
lines = ["# DEX Wallpaper — 276-feature contract", "", "Generated from `tools/feature_names.txt` and explicit `tools/feature_overrides.json`. Run `python3 tools/update_feature_matrix.py` after an evidence update.",
         "", "`DONE_VERIFIED` means the specific behavior has referenced passing evidence. Device acceptance is never inferred from a successful build. Core algorithms and device integrations are tracked separately.","", "## Totals", ""]
lines.extend(f"- **{s}**: {counts[s]}" for s in STATUS)
lines.extend(["- **TOTAL**: 276", "", "## Individual requirements", "", "| ID | Requirement | Implementation | Verification | UI / limitations |", "|---|---|---|---|---|"])
for f in features:
    lines.append(f"| {f['id']} | {f['name']} | {f['implementation_status']} | {f['verification_status']} | {f['ui_location']} — {f['limitations'].replace('|','/')} |")
(ROOT / "FEATURE_MATRIX.md").write_text("\n".join(lines)+"\n")
print(json.dumps(counts,indent=2))
