#!/bin/bash

for i in $@; do (echo $i; awk -F $'\t' '{if ($2 == 100000) {print $3}}' $i) > $i.tmp.txt; done

pr -m -J -t `for i in $@; do echo $i.tmp.txt; done`

for i in $@; do rm -f $i.tmp.txt; done
