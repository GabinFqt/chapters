#!/usr/bin/env bash
# Publish wiki/*.md from this checkout to the GitHub Wiki of GabinFqt/chapters.
# Requires: gh auth, git. Run once after updating wiki/ on both worktrees (keep them identical).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/wiki"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

if [[ ! -d "$SRC" ]]; then
  echo "Missing wiki/ at $SRC" >&2
  exit 1
fi

WIKI_URL="$(gh repo view GabinFqt/chapters --json url -q .url).wiki.git"
echo "Cloning wiki: $WIKI_URL"
if ! git clone --depth 1 "$WIKI_URL" "$TMP/wiki" 2>/dev/null; then
  echo "Wiki clone failed. Create the first page on GitHub (Wiki tab), then re-run." >&2
  exit 1
fi

rsync -a --delete --exclude .git "$SRC/" "$TMP/wiki/"
cd "$TMP/wiki"
git add -A
if git diff --cached --quiet; then
  echo "Wiki already up to date."
  exit 0
fi
git -c user.email="wiki-bot@local" -c user.name="wiki publish" commit -m "Update wiki from repo wiki/"
git push
echo "Wiki published."
