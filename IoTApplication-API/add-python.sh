#!/bin/sh
echo py $1 $2 $3
cd /mnt/data/python
mkdir $1
chmod a+w $1
if [ "$3" = "zip" ]; then
  cd $1
  unzip $2
  cd ..
else 
  cp $2 $1/daScript.py
fi
chmod -R a+rx $1
chown -R 1001.1001 $1
