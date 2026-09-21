#!/bin/bash
[ "$1" = -x ] && shift && set -x
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Install into the repo's own .git/hooks via the relative ../../.git path.
# We deliberately do NOT use `git rev-parse --git-path hooks`: that follows a
# global core.hooksPath, which would (a) drop these per-repo hooks into a
# shared dir and (b) when that dir holds symlinks to a dispatcher, let `cp`
# follow the symlink and clobber the dispatcher's target. A global
# core.hooksPath dispatcher chains to "$GIT_DIR/hooks/<name>", so installing
# into the repo's own .git/hooks is exactly what it expects.
GIT_DIR="${DIR}/../../.git"

# In a git worktree, .git is a file (a gitdir pointer), not a directory; the
# repo's real hooks dir is elsewhere, so skip the copy.
if [ ! -d "${GIT_DIR}" ]; then
    echo "Skipping client hooks install: '${GIT_DIR}' is not a directory (git worktree?)."
    exit 0
fi

# Copy whichever hooks ship next to this script (commit-msg always; pre-commit
# where the repo wires Spotless).
for hook in commit-msg pre-commit; do
    [ -f "${DIR}/${hook}" ] || continue
    cp "${DIR}/${hook}" "${GIT_DIR}/hooks/${hook}"
    chmod +x "${GIT_DIR}/hooks/${hook}"
done

echo 'Client side hooks installed'
ls -al "${GIT_DIR}/hooks"
