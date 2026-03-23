#!/usr/bin/env bash

function downloadHTTP() {
    FILE_NAME=$1
    FILE_SOURCE=$2
    FILE_DEST=$3
    echo "Download from '$FILE_SOURCE'"
    wget -P $FILE_DEST "$FILE_SOURCE/$FILE_NAME"
}
function downloadLocal() {
    FILE_NAME=$1
    FILE_SOURCE=$2
    FILE_DEST=$3

    echo "Copy from '$FILE_SOURCE'"
    cp "$FILE_SOURCE/$FILE_NAME" $FILE_DEST
}

function downloadIfNotExists() {
  FILE_NAME=$1
  FILE_SOURCE=$2
  FILE_DEST=$3
  SOURCE_TYPE=$4
  if [ -f "$FILE_DEST/$FILE_NAME" ]; then
    echo "File '$FILE_DEST/$FILE_NAME' exists and is a regular file."
  else
    echo "File '$FILE_DEST/$FILE_NAME' does not exist or is not a regular file"

    if [[ "$SOURCE_TYPE" == "local" ]]; then
      downloadLocal $FILE_NAME $FILE_SOURCE $FILE_DEST
    elif [[ "$SOURCE_TYPE" == "http" ]]; then
      downloadHTTP $FILE_NAME $FILE_SOURCE $FILE_DEST
    else
      exit 1
    fi
  fi

}