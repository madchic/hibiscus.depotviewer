#!/bin/bash
NAME="DepotViewer"
DJ="${NAME}_lib"
J="${NAME}.jar"
C="bin_build"

rm -Rf "$DJ"
mkdir -p "$DJ"

cp $(cat listJars.lst) "${DJ}/"
JARS=". "$(ls "${DJ}"/*.jar | tr "\n" " ")
#cat manifest_template.txt | sed "s|#CP#|$JARS|" > manifest.txt
cat manifest_template.txt > manifest.txt
echo "Class-Path: $JARS" | fold -w 70 - | sed 's{^{ {;s{ Class-Path:{Class-Path:{' >> manifest.txt
cd "$C"
jar cfm "../$J" "../manifest.txt" .
cd ..

rm -Rf tools/DepotViewer* 
cp -R DepotViewer* tools/
