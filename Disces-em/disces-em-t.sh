#!/bin/bash
while :
do
    /usr/local/bin/python3.6 /home/debian/python-vmstats/disces-em-t.py > /home/debian/python-vmstats/disces-em-t.log 2>&1
    #/home/debian/python-vmstats/disces-em-t 2>&1
done
