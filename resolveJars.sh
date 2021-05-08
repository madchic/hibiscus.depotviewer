#!/bin/bash

LIB="/opt/jameica/ /home/rz/.jameica"

#sudo find $LIB -name "*.idx" -delete
#find $LIB -name "*.jar" -exec sudo ~/dev/hibiscus.depotviewer/index.sh "{}" \;
#find $LIB -name "*.idx" > idxFiles.lst
#JAVAC="javac -sourcepath src -d bin_build src/Tools/TestChart.java"
find src -name "*.java" > src.lst
JAVAC="javac -d bin_build @src.lst"
grep mysql-connector-java idxFiles.lst > listFiles.lst
grep "de.open4me.depot.sql.SQLUtils" $(cat idxFiles.lst) >> listFiles.lst
grep "com.gargoylesoftware.css.parser.CSSErrorHandler" $(cat idxFiles.lst) >> listFiles.lst
rm bin_build/* -R
rm manifest.txt

cd src
tar cf - $(find . -type f -and -not -name "*.java") | tar xf - -C ../bin_build/
cd ..

for j in $(seq 1 10); do
for i in $($JAVAC 2>&1 | grep import | cut -d " " -f2 | sort -u | cut -d";" -f1); do
  e=$(grep $i $(cat idxFiles.lst))
  echo $e >> listFiles.lst
  if [ -z "$e" ]; then
    echo "Not found $i"
  fi
done

cat listFiles.lst | cut -d: -f1 | sort -u | sed 's{\.idx{.jar{' > listJars.lst
export CLASSPATH=$(cat listJars.lst | tr "\n" ":")
echo $CLASSPATH

if [ -e bin_build/Tools/TestChart.class ]; then
   export CLASSPATH=bin_build:$CLASSPATH
   #java Tools.TestChart
   $JAVAC && ./createJar.sh && exit 0; 
   #[ -e manifest.txt ] && $JAVAC && exit 0;
   #[ -e manifest.txt ] || $JAVAC && ./createJar.sh && JAVAC="javac -d bin_build @src.lst";
fi

done
#javac @src.lst -d bin_build
#javac -sourcepath src -d bin_build src/Tools/TestChart.java
$JAVAC &> log.txt
