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
# It creates a bare repository on the first start, imports the declarative
# YAML configuration mounted at /conf and commits it to the main branch. On
# every subsequent start it re-imports the mounted configuration and commits
# only what changed, so the repository history is preserved and stays in sync
# with the mounted directory. Finally the built-in `git daemon` exposes the
# repository over the git:// protocol for lakehouse-config-svc (CVS subsystem)
# on :9418.

set -e

REPO_DIR="/srv/git/config-repo.git"
CONF_SRC="/conf"
BRANCH="main"

# If the repository exists but has no commits yet (e.g. a leftover volume from a
# failed first start), the import is run again so the repo is never served empty.
# Imports the mounted configuration into the repository and pushes it to main.
# Re-runs on every start: if the repo has no commits yet the import becomes a
# root commit, otherwise any difference vs the branch head is committed on top
# of the existing history; a fully in-sync tree results in no commit at all.
import_config() {
  local work
  echo "[git-server] Importing declarative configuration from $CONF_SRC"
  work="/tmp/config-import"
  rm -rf "$work"
  git clone -q "$REPO_DIR" "$work" 2>/dev/null || true
  # if the branch already exists, align the clone with its head BEFORE overlaying
  # the configuration, so a later `add -A` only stages the real differences
  if git -C "$work" rev-parse --verify -q "refs/heads/$BRANCH" >/dev/null; then
    git -C "$work" reset -q --hard "origin/$BRANCH"
  fi
  # cp -a preserves the host ownership of ./conf_git; git running as root then
  # refuses the clone as a "dubious ownership" repository, so restore root ownership
  cp -a "$CONF_SRC"/. "$work"/
  chown -R root:root "$work"
  git -C "$work" config user.name "lakehouse"
  git -C "$work" config user.email "lakehouse@local"
  git -C "$work" add -A
  if git -C "$work" diff --cached --quiet; then
    echo "[git-server] No configuration changes, nothing to commit"
  else
    if git -C "$work" rev-parse --verify -q "refs/heads/$BRANCH" >/dev/null; then
      git -C "$work" commit -m "Update of lakehouse declarative configuration"
    else
      git -C "$work" commit -m "Initial import of lakehouse declarative configuration"
    fi
  fi
  git -C "$work" push -q origin "HEAD:refs/heads/$BRANCH" 2>/dev/null || true
  rm -rf "$work"
}

if [ ! -d "$REPO_DIR" ]; then
  echo "[git-server] Creating bare repository $REPO_DIR (branch $BRANCH)"
  git init --bare --initial-branch="$BRANCH" "$REPO_DIR"
else
  echo "[git-server] Repository $REPO_DIR already initialized on branch $BRANCH"
fi

import_config

echo "[git-server] Starting git daemon on :9418"
exec git daemon --reuseaddr --verbose --base-path=/srv/git --export-all --enable=receive-pack /srv/git