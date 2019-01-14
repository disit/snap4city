#!/bin/sh
echo nr $1
cd /mnt/data/plumber
mkdir $1
chmod a+w $1
cp $2 $1/plumber.R
chmod -R a+rx $1
chown -R 1001.1001 $1
