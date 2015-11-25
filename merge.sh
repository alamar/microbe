#!/bin/bash
# cat seeds.txt | xargs -I_ -P3 mvn exec:java -Dmodel=default -Dseed=_

for i in $@; do (echo $i | sed 's/.txt//'; awk -F $'\t' '{if ($2 == 100000) {print $3}}' $i) > $i.tmp.txt; done

pr -m -J -t `for i in $@; do echo $i.tmp.txt; done`

for i in $@; do rm -f $i.tmp.txt; done
