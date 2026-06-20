#!/usr/bin/env bash

pwd
sudo -u root rm -rf ./storages/minio_storage/*
sudo -u root rm -rf ./storages/minio_storage/.minio.sys
sudo -u root rm -rf ./storages/minio_storage/data
sudo -u root rm -rf ./storages/minio_storage/sparklogs
