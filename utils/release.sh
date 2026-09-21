#!/bin/bash
#
# Cut a release: set the version, commit, tag, then reopen the next -SNAPSHOT.
#
#   ./release.sh 1.0.0                          tags v1.0.0, leaves 1.0.1-SNAPSHOT
#   NEXT_VERSION=1.1.0-SNAPSHOT ./release.sh 1.0.0
#   ./release.sh --dry-run 1.0.0                print the plan, change nothing
#
# Runs against the git repo it is invoked from, so drop it in any Maven
# project root. Nothing is pushed - the push commands are printed for you.

set -euo pipefail

usage() {
  echo "Usage: $(basename "$0") [--dry-run] <version>    e.g. 1.0.0" >&2
  exit 2
}

DRY_RUN=""
if [ "${1:-}" = "--dry-run" ]; then
  DRY_RUN=1
  shift
fi

VERSION="${1:-}"
[ -n "${VERSION}" ] || usage

if ! [[ "${VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Version must be MAJOR.MINOR.PATCH with no suffix, got: ${VERSION}" >&2
  exit 1
fi

# Default to bumping the patch. NEXT_VERSION overrides for a minor/major line.
if [ -n "${NEXT_VERSION:-}" ]; then
  NEXT_SNAPSHOT="${NEXT_VERSION}"
  if [[ "${NEXT_SNAPSHOT}" != *-SNAPSHOT ]]; then
    echo "NEXT_VERSION must end with -SNAPSHOT, got: ${NEXT_SNAPSHOT}" >&2
    exit 1
  fi
else
  IFS='.' read -r major minor patch <<<"${VERSION}"
  NEXT_SNAPSHOT="${major}.${minor}.$((patch + 1))-SNAPSHOT"
fi

ROOT="$(git rev-parse --show-toplevel)"
cd "${ROOT}"

[ -f pom.xml ] || { echo "No pom.xml at repo root: ${ROOT}" >&2; exit 1; }

# Releasing a dirty tree tags something you cannot reproduce from the tag.
if [ -n "$(git status --porcelain)" ]; then
  echo "Working tree is not clean. Commit or stash first." >&2
  exit 1
fi

if git rev-parse -q --verify "refs/tags/v${VERSION}" >/dev/null; then
  echo "Tag v${VERSION} already exists." >&2
  exit 1
fi

MVN="mvn"
[ -x ./mvnw ] && MVN="./mvnw"

set_version() { # set_version <version>
  # A bom module carries its own version and is not a child of the reactor,
  # so it has to be set separately before the root.
  if [ -f bom/pom.xml ]; then
    "${MVN}" -q -f ./bom/pom.xml versions:set versions:update-child-modules \
      -DnewVersion="$1" -DgenerateBackupPoms=false
  fi
  "${MVN}" -q versions:set versions:update-child-modules \
    -DnewVersion="$1" -DgenerateBackupPoms=false
}

if [ -n "${DRY_RUN}" ]; then
  echo "Would release from ${ROOT} using ${MVN}:"
  [ -f bom/pom.xml ] && echo "  bom/pom.xml -> ${VERSION}"
  echo "  pom.xml     -> ${VERSION}"
  echo "  commit      'Release v${VERSION}'"
  echo "  tag         v${VERSION}"
  echo "  pom.xml     -> ${NEXT_SNAPSHOT}"
  echo "  commit      'Open ${NEXT_SNAPSHOT}'"
  exit 0
fi

echo "Releasing v${VERSION}, next snapshot ${NEXT_SNAPSHOT}"

set_version "${VERSION}"
git commit -am "Release v${VERSION}"
git tag -a "v${VERSION}" -m "version ${VERSION}"

set_version "${NEXT_SNAPSHOT}"
git commit -am "Open ${NEXT_SNAPSHOT}"

echo
echo "Done. Nothing pushed yet - review, then:"
echo "  git push && git push origin v${VERSION}"
