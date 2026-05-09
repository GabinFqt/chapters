#!/usr/bin/env bash
# Sync wiki/*.md to the GitHub wiki repo (uses git credentials from `gh auth setup-git`).
# Prerequisite: at least one wiki page must exist on GitHub once (Wiki tab → Create the first page),
# otherwise *.wiki.git returns "Repository not found".

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WIKI_SRC="${ROOT}/wiki"
ORIGIN_URL="$(git -C "${ROOT}" remote get-url origin 2>/dev/null || true)"

if [[ ! -d "${WIKI_SRC}" ]]; then
  echo "missing ${WIKI_SRC}" >&2
  exit 1
fi

derive_repo_slug() {
  local url="$1"
  if [[ "${url}" =~ github\.com[:/]([^/]+)/([^/.]+)(\.git)?$ ]]; then
    echo "${BASH_REMATCH[1]}/${BASH_REMATCH[2]}"
  else
    return 1
  fi
}

if ! REPO="$(derive_repo_slug "${ORIGIN_URL}")"; then
  echo "Could not parse owner/repo from git remote '${ORIGIN_URL}'." >&2
  exit 1
fi

OWNER="${REPO%%/*}"
REPO_NAME="${REPO##*/}"
WIKI_URL="https://github.com/${OWNER}/${REPO_NAME}.wiki.git"
FIRST_PAGE="https://github.com/${OWNER}/${REPO_NAME}/wiki"

if ! command -v gh >/dev/null 2>&1; then
  echo "Install the GitHub CLI: https://cli.github.com/" >&2
  exit 1
fi

gh auth setup-git

TMP="$(mktemp -d)"
cleanup() {
  rm -rf "${TMP}"
}
trap cleanup EXIT

echo "→ Cloning wiki: ${WIKI_URL}"

if ! git clone "${WIKI_URL}" "${TMP}/wiki" 2>/dev/null; then
  echo "" >&2
  echo "Clone failed — the wiki Git remote does not exist until you create the first page:" >&2
  echo "  ${FIRST_PAGE}" >&2
  echo "After saving one page there, rerun: $0" >&2
  exit 1
fi

cp "${WIKI_SRC}"/*.md "${TMP}/wiki/"

cd "${TMP}/wiki"
git add .

if git diff --cached --quiet; then
  echo "Wiki already up to date (no staged changes)."
  exit 0
fi

git commit -m "docs(wiki): sync from repo wiki/"

CURRENT="$(git symbolic-ref --short HEAD 2>/dev/null || true)"
[[ -z "${CURRENT}" ]] && CURRENT=master

echo "→ pushing to ${CURRENT} ..."
git push origin "${CURRENT}"

echo "Done. Open: ${FIRST_PAGE}"
