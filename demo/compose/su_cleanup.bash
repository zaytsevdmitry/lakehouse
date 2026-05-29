#!/usr/bin/env bash

pwd
sudo -u root rm -rf ./storages/minio_storage/*
sudo -u root rm -rf ./storages/minio_storage/.minio.sys
sudo -u root rm -rf ./logs/spark/master/*
sudo -u root rm -rf ./logs/spark/worker1/*
sudo -u root rm -rf ./logs/spark/history/*