#!/bin/bash
set -e

# Configuration
VERSION="0.10.0"
TMP_DIR="/tmp/lakehouse_k8s_conf_${VERSION}"
LOCAL_CONF_SRC="./conf_git"
NAMESPACE="lakehouse-management"

echo "--> 1. Ensuring git-server pod is ready..."
kubectl wait -n "$NAMESPACE" --for=condition=ready pod -l app=git-server --timeout=60s

echo "--> 2. Refreshing port-forward tunnel for git-server to avoid dead sessions..."
# Kill any existing local process holding port 9418 to avoid "port already in use" errors
SUICIDE_PORT=$(lsof -t -i:9418 || true)
if [ ! -z "$SUICIDE_PORT" ]; then
    kill -9 $SUICIDE_PORT 2>/dev/null || true
fi

# Open a fresh guaranteed tunnel to the current active pod
kubectl port-forward -n "$NAMESPACE" svc/git-server 9418:9418 > /dev/null 2>&1 &
PID_PF=$!

# Ensure this specific temporary tunnel is closed when script ends
cleanup() {
  echo "--> Cleaning up temporary git tunnel (PID: $PID_PF)..."
  kill $PID_PF 2>/dev/null || true
}
trap cleanup EXIT

sleep 2

echo "--> 3. Preparing temporary directory: $TMP_DIR"
rm -rf "$TMP_DIR"
mkdir -p "$TMP_DIR"

echo "--> 4. Copying source configuration files..."
if [ -d "$LOCAL_CONF_SRC" ] && [ "$(ls -A $LOCAL_CONF_SRC)" ]; then
    cp -r "$LOCAL_CONF_SRC"/. "$TMP_DIR/"
else
    echo "Error: Local source directory '$LOCAL_CONF_SRC' is empty or does not exist!"
    exit 1
fi

echo "--> 5. Initializing local Git repository and preparing commit..."
cd "$TMP_DIR"
git init -b main

# Set local repository identity to prevent overriding global git configs
git config user.email "bootstrap@lakehouse.local"
git config user.name "Bootstrap Agent v${VERSION}"

git add .
git commit -m "Bootstrap configurations for version ${VERSION}" || echo "Notice: No changes to commit"

echo "--> 6. Attaching remote repository destination via forwarded port..."
git remote add origin git://127.0.0.1:9418/config-repo.git

echo "--> 7. Pushing configuration files to the git-server..."
# Force push allows updating/overriding configurations on repetitive runs
git push origin main --force

echo "--> Success: Lakehouse configurations version ${VERSION} pushed successfully."
