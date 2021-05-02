#!/bin/bash

TMP=classpath_template

cp classpath_template .classpath

for i in $(cat listJars.lst); do
  a="        <classpathentry exported=\"true\" kind=\"lib\" path=\"$i\"/>"
  sed "s{<!-- ## -->{$a\n<!-- ## -->{" -i .classpath
done

