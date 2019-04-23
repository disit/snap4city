#!/bin/sh
echo pt $1
cd /mnt/data/portia
mkdir $1
mkdir $1/projects
mkdir $1/outs

chmod a+w $1
chmod -R a+rx $1
chown -R 1001.1001 $1
