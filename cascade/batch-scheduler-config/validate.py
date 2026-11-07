#!/usr/bin/env python3
"""Shape + dependency check for jobs/*.yaml (no PyYAML needed: the files use a flat subset)."""
import re
import sys
from pathlib import Path

JOBS = Path(__file__).resolve().parent / "jobs"
REQUIRED = {"job", "owner", "schedule", "connection", "engine", "depends_on", "on_failure", "migration"}
KNOWN_PROCS = {"pkg_interest_engine.accrue_daily_interest", "pkg_interest_engine.capitalize_interest",
               "pkg_fee_processing.assess_monthly_fees", "pkg_gl_posting.gl_balance", "pkg_gl_posting.gl_balance_summary"}


def parse(path: Path) -> dict:
    top = {}
    for line in path.read_text().splitlines():
        m = re.match(r"^([a-z_]+):\s*(.*?)\s*(#.*)?$", line)
        if m:
            top[m.group(1)] = m.group(2)
    deps = re.findall(r"[\w-]+", top.get("depends_on", "[]"))
    top["_deps"] = deps
    top["_procs"] = set(re.findall(r"pkg_\w+\.\w+", path.read_text()))
    return top


def main() -> int:
    jobs = {p.stem: parse(p) for p in sorted(JOBS.glob("*.yaml"))}
    errors = []
    for name, j in jobs.items():
        missing = REQUIRED - set(j)
        if missing:
            errors.append(f"{name}: missing {sorted(missing)}")
        if j.get("job") != name:
            errors.append(f"{name}: job field '{j.get('job')}' != file name")
        for d in j["_deps"]:
            if d not in jobs:
                errors.append(f"{name}: depends_on unknown job {d}")
        for p in j["_procs"] - KNOWN_PROCS:
            errors.append(f"{name}: invokes {p}, not in the tranche-1 inventory")
    # dependency order must be acyclic
    seen, order = set(), []

    def visit(n, stack):
        if n in stack:
            errors.append(f"cycle at {n}")
            return
        if n in seen:
            return
        for d in jobs[n]["_deps"]:
            visit(d, stack | {n})
        seen.add(n)
        order.append(n)

    for n in jobs:
        visit(n, frozenset())
    for e in errors:
        print("ERROR", e)
    print(f"{len(jobs)} jobs, order: {' -> '.join(order)}")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
