# -*- coding: utf-8 -*-
"""Embed api-catalog into full-workflow Part III."""
from pathlib import Path

ROOT = Path(__file__).resolve().parent
PLAYBOOK = ROOT / "full-workflow-api-test-gd1-gd6.md"
CATALOG = ROOT / "api-catalog-with-json.md"

playbook = PLAYBOOK.read_text(encoding="utf-8")
catalog = CATALOG.read_text(encoding="utf-8")

# Catalog body: skip title + link back to playbook
cat_lines = catalog.splitlines()
start = 0
for i, line in enumerate(cat_lines):
    if line.startswith("## GĐ") or line.startswith("## System"):
        start = i
        break
cat_body = "\n".join(cat_lines[start:])

PART3_HEADER = """# Phần III — Catalog API (request/response JSON)

> **166 endpoint** — mỗi khối gồm Method + Path, Request JSON, Response `data`.  
> Envelope 2xx: `{ success, data, message?, traceId, timestamp }`.  
> Tìm nhanh: `Ctrl+F` path (vd. `POST /api/v1/submissions`) hoặc mã `3.012`.

"""

PART4_MARKER = "# Phần IV — Checklist nhanh"

i3 = playbook.find("# Phần III — Catalog API")
i4 = playbook.find(PART4_MARKER)
if i3 < 0 or i4 < 0:
    raise SystemExit("Could not find Part III / IV markers in playbook")

merged = playbook[:i3] + PART3_HEADER + cat_body + "\n\n---\n\n" + playbook[i4:]
PLAYBOOK.write_text(merged, encoding="utf-8")
print("merged", len(merged.splitlines()), "lines into", PLAYBOOK.name)
