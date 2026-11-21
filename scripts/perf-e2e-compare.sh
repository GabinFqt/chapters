#!/usr/bin/env bash
# Compare server and client perf runs on two refs.
# Usage: ./scripts/perf-e2e-compare.sh <before-ref> <after-ref> <commit> [commit...]
# Each commit is cherry-picked, in order, onto both refs inside a temporary worktree.
# A conflict stops the script. The worktrees are deleted at the end.
set -euo pipefail

if [ "$#" -lt 3 ]; then
  echo "usage: $0 <before-ref> <after-ref> <commit> [commit...]" >&2
  exit 2
fi

before=$1
after=$2
shift 2
root=$(git rev-parse --show-toplevel)
out=$root/build/perf-e2e
rm -rf "$out"
mkdir -p "$out"

run_ref() {
  local label=$1
  local ref=$2
  shift 2
  local dir
  dir=$(mktemp -d /tmp/chapters-perf-XXXX)
  git worktree add --detach "$dir" "$ref"
  (
    cd "$dir"
    for commit in "$@"; do
      if ! git cherry-pick "$commit"; then
        echo "cherry-pick of $commit failed on $ref" >&2
        exit 1
      fi
    done
    local i
    for i in 1 2 3; do
      ./gradlew runPerfGameTestServer --no-daemon
      cp run-perf/perf-server.tsv "$out/${label}-server-${i}.tsv"
      if command -v xvfb-run >/dev/null 2>&1; then
        xvfb-run -a --server-args="-screen 0 1280x720x24" ./gradlew runClientPerf --no-daemon
      else
        ./gradlew runClientPerf --no-daemon
      fi
      cp run-client-perf/perf-client.tsv "$out/${label}-client-${i}.tsv"
    done
  )
  git worktree remove --force "$dir"
}

run_ref before "$before" "$@"
run_ref after "$after" "$@"

python3 - "$out" <<'PY'
import sys
from pathlib import Path
from statistics import median

out = Path(sys.argv[1])

def load(label, kind):
    merged = {}
    for path in sorted(out.glob(f"{label}-{kind}-*.tsv")):
        for line in path.read_text().splitlines():
            if not line.strip():
                continue
            name, value = line.split("\t", 1)
            merged.setdefault(name, []).append(float(value))
    return {name: median(values) for name, values in merged.items()}

def write(kind):
    before = load("before", kind)
    after = load("after", kind)
    names = list(dict.fromkeys([*before.keys(), *after.keys()]))
    lines = [f"## {kind}", "", "| Measure | Before | After | Delta |", "| --- | ---: | ---: | ---: |"]
    for name in names:
        b = before.get(name)
        a = after.get(name)
        if b is None or a is None or b == 0:
            delta = ""
        else:
            delta = f"{(a - b) / b * 100:+.1f}%"
        lines.append(f"| {name} | {'' if b is None else f'{b:.3f}'} | {'' if a is None else f'{a:.3f}'} | {delta} |")
    return "\n".join(lines)

text = "# Chapters e2e perf\n\nMedian of 3 runs.\n\n" + write("server") + "\n\n" + write("client") + "\n"
(out / "compare.md").write_text(text)
print(text)
PY
