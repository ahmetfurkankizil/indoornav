import json
import os

docs_root = r"c:\Users\emirh\Desktop\bitirme\VecturAI\codebase-docs"

with open(os.path.join(docs_root, "codebase-index.json"), "r") as f:
    inventory = json.load(f)

files = inventory["files"]
folders = inventory["folders"]

status_counts = {
    "mapped": 0,
    "indexed_minimal": 0,
    "skipped_with_reason": 0,
    "unresolved": 0
}

for f in files:
    status_counts[f["status"]] += 1

unresolved_list = [f["path"] for f in files if f["status"] == "unresolved"]
skipped_list = [(f["path"], f["reason"]) for f in files if f["status"] == "skipped_with_reason"]

report = f"""# Coverage Report

## Summary
- **Total Folders**: {len(folders)}
- **Total Files**: {len(files)}
- **Mapped**: {status_counts['mapped']}
- **Indexed Minimal**: {status_counts['indexed_minimal']}
- **Skipped with Reason**: {status_counts['skipped_with_reason']}
- **Unresolved**: {status_counts['unresolved']}

## Completion Verdict
- **Status**: Initializing (Pass 1 complete)
- **Progress**: 0% (Full mapping pending)

## Skipped List
"""
for path, reason in skipped_list:
    report += f"- `{path}`: {reason}\n"

report += "\n## Unresolved Inventory (Sample)\n"
for path in unresolved_list[:20]:
    report += f"- `{path}`\n"
if len(unresolved_list) > 20:
    report += f"- ... and {len(unresolved_list) - 20} more.\n"

with open(os.path.join(docs_root, "coverage-report.md"), "w") as f:
    f.write(report)
