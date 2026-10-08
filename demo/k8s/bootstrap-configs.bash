#!/bin/bash
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

# Bootstrap of the demo configuration repositories of the in-cluster git-server.
#
# Every directory under conf_git/domains is served from its own bare repository
# named after that directory, the same way the compose demo does it
# (demo/compose/conf_infra/git/git-server-init.sh):
#
#     conf_git/domains/platform -> git://git-server:9418/platform.git
#
# The domain is deliberately not part of the declarative file content: the
# configuration service derives it from the repository it synchronizes and stamps
# it onto every loaded object (GitOpsSynchronizer#stampDomain), which is why the
# repository has to be named after the domain.
#
# The git-server storage is an emptyDir, so the bare repositories are created on
# every pod start and re-pushed by this script on every run. History is preserved:
# a domain commit is built on top of the current branch head, so a run that changes
# nothing creates no commit, and a run that adds or removes a file records exactly
# that difference.

set -e

# Configuration
VERSION="0.11.0"
TMP_DIR="/tmp/lakehouse_k8s_conf_${VERSION}"
# Resolved from the script location, so it can be run from any working directory.
# The configuration tree is shared with demo/compose and therefore lives next to
# the k8s directory; set CONF_GIT_SRC to read it from somewhere else.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOCAL_CONF_SRC="${CONF_GIT_SRC:-${SCRIPT_DIR}/../conf_git}"
DOMAINS_SRC="${LOCAL_CONF_SRC}/domains"
NAMESPACE="lakehouse-management"
GIT_POD_LABEL="app=git-server"
# Repository root inside the git-server container, the export root of `git daemon`.
GIT_REPO_ROOT="/srv/git"
GIT_PORT="9418"
BRANCH="main"
# Spark tasks of the platform domain reference the executor jar as a file: resource,
# which makes the submission service download it from the outside. In the cluster the
# jar is mounted into every spark driver, so the configuration has to declare it as a
# local: resource instead. demo/compose reads the very same configuration tree and
# needs file:, hence the scheme is rewritten in the copies made into the temporary work
# tree, never in the sources.
APP_RESOURCE_FILES="sparkTemplate.yaml sparkDQTemplate.yaml"
# Legacy single-repository import: the whole conf_git tree, domains/ prefix included.
# Kept so that a configuration service still pointed at
# lakehouse.config.vcs.git.repository-url=.../config-repo.git keeps working; clear it
# once the chart declares one repository per domain, like demo/compose does.
LEGACY_REPO="config-repo.git"

GIT_POD=""

cleanup() {
    if [ -n "$PID_PF" ]; then
        echo "--> Cleaning up temporary git tunnel (PID: $PID_PF)..."
        kill "$PID_PF" 2>/dev/null || true
    fi
}

# Creates the bare repository of one domain inside the git-server container. The
# container only pre-creates the legacy repository on start, every further one is
# created here, so the daemon has something to receive the push into.
ensure_bare_repo() {
    local name="$1"
    kubectl -n "$NAMESPACE" exec -i "$GIT_POD" -- sh -s -- "$name" <<REMOTE
set -e
name="\$1"
repo="${GIT_REPO_ROOT}/\$name.git"
if [ ! -f "\$repo/HEAD" ]; then
    echo "[git-server] Creating bare repository \$repo (branch ${BRANCH})"
    mkdir -p "\$repo"
    git init -q --bare --shared --initial-branch=${BRANCH} "\$repo"
    touch "\$repo/git-daemon-export-ok"
else
    echo "[git-server] Repository \$repo already exists"
fi
# `git init` leaves HEAD on refs/heads/master, which never receives the branch this
# script pushes; a dangling HEAD makes JGit clones fail in the configuration service.
git -C "\$repo" symbolic-ref HEAD refs/heads/${BRANCH} || true
# Allow the force push below, so a re-run can rewrite the branch head.
git -C "\$repo" config receive.denyNonFastForwards false
REMOTE
}

# Rewrites deploy.appResource: file: into deploy.appResource: local: in the spark task
# definitions of the copied work tree, so that the jars mounted into the spark drivers
# are referenced as local resources instead of being downloaded by the submission
# service. Takes the work tree as the argument; both layouts are covered, the tasks of
# a domain repository at <domain>/tasks/ and the same tasks inside the legacy
# repository at domains/<domain>/tasks/. Idempotent, therefore a run that changes
# nothing in the sources still creates no commit.
rewrite_app_resource() {
    local work="$1"
    local name
    local file
    local replaced

    for name in $APP_RESOURCE_FILES; do
        for file in "$work"/*/tasks/"$name" "$work"/*/*/tasks/"$name"; do
            [ -f "$file" ] || continue
            replaced=$(grep -c -F "deploy.appResource: file:" "$file" || true)
            if [ "$replaced" -eq 0 ]; then
                continue
            fi
            sed -i 's|^\([[:space:]]*deploy\.appResource:[[:space:]]*\)file:|\1local:|' "$file"
            echo "-->    ${file#"$work"/}: deploy.appResource switched to local: (${replaced})"
        done
    done
}

# Imports the files of one directory into the bare repository of the same name and
# pushes them to the main branch. Accepts the repository name and the source
# directory as arguments; re-runs on every invocation without duplicating history.
import_repo() {
    local repo_name="$1"
    local src="$2"
    local name="${repo_name%.git}"
    local work="${TMP_DIR}/${repo_name}"
    local remote="git://127.0.0.1:${GIT_PORT}/${repo_name}"

    if [ ! -d "$src" ]; then
        echo "Error: source directory '$src' does not exist!"
        exit 1
    fi
    if [ -z "$(ls -A "$src")" ]; then
        echo "Error: source directory '$src' is empty!"
        exit 1
    fi

    ensure_bare_repo "$name"

    rm -rf "$work"
    git init -q -b "$BRANCH" "$work"
    # Set the local repository identity to prevent overriding global git configs
    git -C "$work" config user.email "bootstrap@lakehouse.local"
    git -C "$work" config user.name "Bootstrap Agent v${VERSION}"
    git -C "$work" remote add origin "$remote"

    # Continue the existing history when the branch is already there, start a new one
    # otherwise. The work tree is replaced below either way.
    if git -C "$work" fetch -q origin "$BRANCH" 2>/dev/null; then
        git -C "$work" checkout -q -B "$BRANCH" FETCH_HEAD
    else
        git -C "$work" checkout -q -B "$BRANCH"
    fi
    # Drop everything but .git so that files removed from the source disappear from
    # the repository as well
    find "$work" -mindepth 1 -maxdepth 1 ! -name .git -exec rm -rf {} +
    cp -a "$src"/. "$work"/
    # Adapt the copies of the spark tasks to the cluster before they are committed
    rewrite_app_resource "$work"

    git -C "$work" add -A
    if git -C "$work" diff --cached --quiet; then
        echo "--> ${repo_name}: already in sync, nothing to commit"
    else
        git -C "$work" commit -q -m "Bootstrap configurations for version ${VERSION} (${name})"
        echo "--> ${repo_name}: configuration committed"
    fi
    git -C "$work" push -q --force origin "HEAD:refs/heads/${BRANCH}"
}

echo "--> 1. Ensuring git-server pod is ready..."
kubectl wait -n "$NAMESPACE" --for=condition=ready pod -l "$GIT_POD_LABEL" --timeout=60s
GIT_POD=$(kubectl -n "$NAMESPACE" get pod -l "$GIT_POD_LABEL" -o jsonpath='{.items[0].metadata.name}')

echo "--> 2. Refreshing port-forward tunnel for git-server to avoid dead sessions..."
# Kill any existing local process holding the port to avoid "port already in use" errors
SUICIDE_PORT=$(lsof -t -i:"$GIT_PORT" || true)
if [ ! -z "$SUICIDE_PORT" ]; then
    kill -9 $SUICIDE_PORT 2>/dev/null || true
fi

# Open a fresh guaranteed tunnel to the current active pod
kubectl port-forward -n "$NAMESPACE" svc/git-server "$GIT_PORT:$GIT_PORT" > /dev/null 2>&1 &
PID_PF=$!

# Ensure this specific temporary tunnel is closed when script ends
trap cleanup EXIT

sleep 2

echo "--> 3. Discovering configuration domains in '${DOMAINS_SRC}'..."
if [ ! -d "$DOMAINS_SRC" ]; then
    echo "Error: domains directory '$DOMAINS_SRC' does not exist!"
    echo "       expected the configuration tree at '$LOCAL_CONF_SRC'"
    echo "       set CONF_GIT_SRC to point at a different location"
    exit 1
fi
DOMAINS=()
for dir in "$DOMAINS_SRC"/*/; do
    [ -d "$dir" ] || continue
    DOMAINS+=("$(basename "$dir")")
done
if [ ${#DOMAINS[@]} -eq 0 ]; then
    echo "Error: no domain directories found in '$DOMAINS_SRC'!"
    exit 1
fi
echo "-->    domains found: ${DOMAINS[*]}"

echo "--> 4. Preparing temporary directory: $TMP_DIR"
rm -rf "$TMP_DIR"
mkdir -p "$TMP_DIR"

echo "--> 5. Creating and filling one repository per domain on the git-server..."
for domain in "${DOMAINS[@]}"; do
    import_repo "${domain}.git" "${DOMAINS_SRC}/${domain}"
done

if [ -n "$LEGACY_REPO" ]; then
    echo "--> 6. Importing the legacy single repository '${LEGACY_REPO}'..."
    import_repo "$LEGACY_REPO" "$LOCAL_CONF_SRC"
    IMPORTED="${DOMAINS[*]} ${LEGACY_REPO}"
else
    IMPORTED="${DOMAINS[*]}"
fi

echo "--> Success: repositories ${IMPORTED} pushed to git-server successfully."
