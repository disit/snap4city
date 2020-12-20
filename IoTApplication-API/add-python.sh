#!/bin/sh
echo py $1 $2
cd /mnt/data/python
mkdir $1
chmod a+w $1
cp $2 $1/daScript.py
chmod -R a+rx $1
chown -R 1001.1001 $1
