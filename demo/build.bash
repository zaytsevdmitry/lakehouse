#!/usr/bin/env bash
set -e
set -v
export VERSION=0.4.0
cd  ../docker
bash ./build.bash

docker images | grep lakehouse