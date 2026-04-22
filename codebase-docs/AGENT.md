# AGENT.md

## Purpose

This repository includes a generated **codebase context pack** intended to let agents understand the system quickly without re-discovering the repository from scratch.

The context pack is a navigation and orientation layer, not a replacement for source-of-truth code. Use it to narrow scope, identify ownership and runtime flow, and jump directly to the most relevant files.

---

## What you are looking at

The repository contains generated documentation artifacts that describe the codebase at three levels:

1. **Top-level map** — repository-wide architecture and navigation
2. **Feature maps** — subsystem and feature-level implementation context
3. **File dossiers** — detailed file-level context with symbols, line ranges, and cross-references

These artifacts are backed by a machine-readable index and a coverage report so agents can tell what has been mapped, what has only been minimally indexed, and what remains unresolved.

---

## Expected artifact layout

Unless the project uses a different docs root, expect something close to:

```text
/docs/context/
  codebase-map.md
  coverage-report.md
  codebase-index.json
  workplan.md
  repo-tree.md
  features/
    feature-*.md
  files/
    *.md
```

If the docs root differs, locate the files with these same names.

---

## Reading order

When starting any task, use this order:

### 1. Read `codebase-map.md`
Use it to understand:
- repository purpose
- tech stack
- entrypoints
- runtime architecture
- build/run/test flow
- folder responsibilities
- external integrations
- feature index

This is the default starting point.

### 2. Read the relevant `features/feature-*.md`
Use feature docs to understand:
- which files implement a feature
- which other files/features use it
- execution flow
- key symbols
- config/env flags
- related tests

Use these before opening many raw source files.

### 3. Read the relevant `files/*.md` dossiers
Use file dossiers when you need:
- exact file role
- imports/includes
- exports/public surface
- main symbols
- line-range summaries
- uses / used-by relationships
- config/protocol details
- related tests

### 4. Read `coverage-report.md` if completeness matters
Use it to verify:
- whether the mapping is complete
- whether specific files were only minimally indexed
- which paths were skipped and why
- which paths remain unresolved

### 5. Read `codebase-index.json` for machine lookup and narrow retrieval
Use the index when you need to answer:
- where is X implemented?
- what uses Y?
- which feature owns this file?
- which docs point to this path?
- which files are unresolved or minimally indexed?

---

## Artifact semantics

### `codebase-map.md`
Human-readable top-level architecture map.

### `features/feature-*.md`
Feature or subsystem documents. These are the best entrypoint for feature work.

### `files/*.md`
Detailed file dossiers for authored, behavior-relevant files.

### `codebase-index.json`
Machine-readable master index of all known paths and their mapping status.

### `coverage-report.md`
Coverage and completeness ledger. This is the anti-silent-skip artifact.

### `repo-tree.md`
Readable inventory/tree reference.

### `workplan.md`
Mapping batches and traversal plan. Mainly useful for regeneration, resumption, or audit.

---

## Status meanings

Each path in `codebase-index.json` should have one of these statuses:

### `mapped`
The path has been meaningfully analyzed. For authored behavior-relevant files, this should normally mean there is a corresponding file dossier.

### `indexed_minimal`
The path is accounted for, but only lightly documented. Typical for generated files, assets, lockfiles, vendor code, build output, or low-value support artifacts.

### `skipped_with_reason`
The path was intentionally not deeply mapped and the reason is explicit.

### `unresolved`
The path or its relationships were not fully resolved. Treat this as incomplete context and inspect raw code directly.

---

## Operating rules for agents

### Start from the context pack, not from raw repo discovery
Do not begin by recursively re-scanning the repository unless the context pack is missing, stale, or incomplete.

### Use the top-down navigation model
Start broad, then narrow:
1. `codebase-map.md`
2. relevant feature docs
3. relevant file dossiers
4. raw source files only after narrowing scope

### Treat source code as the final source of truth
The context pack is meant to accelerate orientation, not replace verification.

### Respect coverage status
If a file is marked `indexed_minimal`, `skipped_with_reason`, or `unresolved`, do not over-trust the generated docs. Inspect the underlying file directly.

### Prefer existing feature boundaries
When reasoning about ownership or runtime flow, prefer the subsystem/feature boundaries established in the feature docs unless the raw code disproves them.

### Use `Used By` as a fast impact-analysis tool
When changing a file or symbol, check who depends on it before editing.

### Use line-range summaries to target inspection
The dossier line ranges are there to reduce reading cost. Use them to jump to the right region of code.

---

## Common task routing

### “Where is this feature implemented?”
1. Search `codebase-map.md` feature index
2. Open matching `features/feature-*.md`
3. Follow linked file dossiers
4. Verify in raw source

### “What uses this module/file?”
1. Open the relevant file dossier
2. Read the `Used By` section
3. Cross-check with `codebase-index.json`
4. Verify raw call sites if the change is risky

### “Which config affects this behavior?”
1. Check `codebase-map.md` build/run/config sections
2. Check feature doc config/env section
3. Check file dossier config/constants section
4. Verify the actual config/build files

### “Is this area fully mapped?”
1. Open `coverage-report.md`
2. Check status in `codebase-index.json`
3. Inspect unresolved/skipped entries before trusting the docs

### “What should I read first for a new task?”
1. `codebase-map.md`
2. the most relevant feature doc
3. linked file dossiers
4. only then raw code

---

## When to distrust the generated context

Be cautious when:
- the relevant path is marked `unresolved`
- the relevant path is only `indexed_minimal`
- the repository changed after the context pack was generated
- the task depends on exact runtime behavior, protocol edge cases, build flags, or macro/template expansion
- generated code or codegen inputs are involved
- reflection, registration, DI, annotations, or dynamic loading are heavily used

In these cases, use the context pack only to narrow scope, then inspect the source directly.

---

## Update protocol after code changes

If you modify the codebase, the context pack should be updated as well.

Minimum update expectations:
- update affected file dossiers
- update affected feature docs
- update `codebase-map.md` if architecture, flow, ownership, or entrypoints changed
- update `codebase-index.json` for new, moved, renamed, or removed files
- update `coverage-report.md` if coverage status changed

If the changes are large, rerun the mapping workflow incrementally rather than patching docs manually.

---

## Completion invariant

The context pack should only be considered complete when:
- every folder and file in the repository inventory is present in `codebase-index.json`
- every authored behavior-relevant file is either `mapped` with a dossier or explicitly not deeply mapped with reason
- no path is silently omitted
- unresolved items are explicitly listed
- `coverage-report.md` states a justified final verdict

---

## Recommended agent behavior summary

Use this compact policy:

1. Read `codebase-map.md` first.
2. Narrow to the relevant feature doc.
3. Read the linked file dossiers.
4. Check coverage/status before trusting the docs.
5. Verify critical behavior in raw source.
6. When you change code, update the context pack.

That is the intended workflow.
