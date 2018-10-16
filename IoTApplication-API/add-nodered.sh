#!/bin/sh
echo nr $1
cd /mnt/data/nr-data
mkdir $1
chmod a+w $1
sed -e "s/__NRID__/$1/g" settings.js.tpl | sed -e "s/__USERNAME__/$2/g" > $1/settings.js
cp flows-*.json $1
chown -R 1001.1001 $1
