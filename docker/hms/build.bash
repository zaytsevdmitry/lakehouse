#!/usr/bin/env bash
set -e
export LH_VERSION=0.3.0

docker build -t lakehouse-hms:$LH_VERSION ./
docker images
