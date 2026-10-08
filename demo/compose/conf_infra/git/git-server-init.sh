#!/bin/sh
# "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0.txt
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Bootstrap script of the demo configuration repository server.
#
# Every domain kept under /conf/domains is served from its own bare repository
# named after the directory (e.g. /conf/domains/platform -> /srv/git/platform.git).
# On the first start import_config() creates the repository and imports the
# declarative YAML configuration of the domain into branch main. On every
# subsequent start it re-imports the mounted configuration and commits only what
# changed, so the repository history is preserved and stays in sync with the
# mounted directory. Finally the built-in `git daemon` exposes all repositories
# over the git:// protocol for lakehouse-config-svc (CVS subsystem) on :9418.

set -e

DOMAINS_SRC="${DOMAINS_SRC:-/conf/domains}"
REPO_ROOT="${REPO_ROOT:-/srv/git}"
BRANCH="main"

# Imports the files of one domain into its own bare repository and pushes them
# to main. Accepts as the single argument the path of the directory containing
# the declarative configuration files; the repository is created at
# $REPO_ROOT/<dirname>.git if it does not exist yet. Re-runs on every start:
# if the repository has no commits yet the import becomes a root commit,
# otherwise any difference vs the branch head is committed on top of the
# existing history; a fully in-sync tree results in no commit at all.
import_config() {
  local src name repo work
  src="$1"
  [ -n "$src" ] || { echo "[git-server] import_config: source path is required"; return 1; }
  [ -d "$src" ] || { echo "[git-server] import_config: source $src is not a directory"; return 1; }

  name="$(basename "$src")"
  repo="$REPO_ROOT/$name.git"
  echo "[git-server] Importing domain '$name' from $src"

  if [ ! -d "$repo" ]; then
    echo "[git-server] Creating bare repository $repo (branch $BRANCH)"
    git init -q --bare --initial-branch="$BRANCH" "$repo"
  else
    echo "[git-server] Repository $repo already initialized on branch $BRANCH"
  fi

  work="/tmp/config-import-$name"
  rm -rf "$work"
  git clone -q "$repo" "$work" 2>/dev/null || true
  # if the branch already exists, align the clone with its head BEFORE overlaying
  # the configuration, so a later `add -A` only stages the real differences
  if git -C "$work" rev-parse --verify -q "refs/heads/$BRANCH" >/dev/null; then
    git -C "$work" reset -q --hard "origin/$BRANCH"
  fi
  # wipe the work tree (but keep .git) and copy the source over it, so files
  # removed from the source are also removed from the repository
  find "$work" -mindepth 1 -maxdepth 1 ! -name .git -exec rm -rf {} +
  # cp -a preserves the host ownership of ./conf_git; when running as root git
  # refuses the clone as a "dubious ownership" repository, so restore root ownership
  cp -a "$src"/. "$work"/
  if [ "$(id -u)" = 0 ]; then
    chown -R root:root "$work"
  fi
  git -C "$work" config user.name "lakehouse"
  git -C "$work" config user.email "lakehouse@local"
  git -C "$work" add -A
  if git -C "$work" diff --cached --quiet; then
    echo "[git-server] Domain '$name': no configuration changes, nothing to commit"
  else
    if git -C "$work" rev-parse --verify -q "refs/heads/$BRANCH" >/dev/null; then
      git -C "$work" commit -q -m "Update of lakehouse declarative configuration ($name)"
    else
      git -C "$work" commit -q -m "Initial import of lakehouse declarative configuration ($name)"
    fi
  fi
  git -C "$work" push -q origin "HEAD:refs/heads/$BRANCH" 2>/dev/null || true
  rm -rf "$work"
}

# Imports every domain directory mounted at $DOMAINS_SRC into its own
# same-named repository.
import_domains() {
  if [ ! -d "$DOMAINS_SRC" ]; then
    echo "[git-server] No $DOMAINS_SRC directory found, nothing to import"
    return 0
  fi
  for domain in "$DOMAINS_SRC"/*; do
    [ -d "$domain" ] || continue
    import_config "$domain"
  done
}

import_domains

echo "[git-server] Starting git daemon on :9418"
exec git daemon --reuseaddr --verbose --base-path="$REPO_ROOT" --export-all --enable=receive-pack "$REPO_ROOT"