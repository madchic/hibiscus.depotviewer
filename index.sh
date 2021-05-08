#!/bin/bash

FILE=$1
IDX=$(echo $FILE | sed 's{\.jar${.idx{')

if [ "$FILE" == "$IDX" ]; then
  echo "Unable to process file $FILE"
  exit -1
fi

unzip -t "$FILE" | grep \.class | cut -d: -f2 | cut -d " " -f2 | sed 's{\.class{{;s{/{.{g' > "$IDX"
