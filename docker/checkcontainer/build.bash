#!/usr/bin/env bash
set -e
export LH_VERSION=0.4.0

docker build -t lakehouse-s3-check:$LH_VERSION ./
docker images
