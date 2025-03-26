#!/bin/bash
echo $1
mkdir -p cache
rm cache/*

for f in $1/*.n3
do
  echo $f
  if [[ $(find $f -type f -size +50M 2>/dev/null) ]]; then
    echo split $s
    #mv $f $f.orig
    split -l 100000 --additional-suffix=.n3 $f cache/${f##*/}
  else
    cp $f cache/${f##*/}
  fi
done

echo $2 removing graph $3
curl -s --digest --user ${VIRTOSO_USER:-dba}:${VIRTUOSO_PWD:-dba} -X DELETE --url "http://$2:8890/sparql-graph-crud-auth?graph-uri=$3"
#isql-vt -U ${VIRTOSO_USER:-dba} -P ${VIRTUOSO_PWD:-dba} -H $2 "EXEC=SPARQL DEFINE sql:log-enable 3 CLEAR GRAPH <$3>"

echo $2 sending data

for f in cache/*.n3
do
  echo $f
  curl -s --digest --user ${VIRTOSO_USER:-dba}:${VIRTUOSO_PWD:-dba} -X POST --url "http://$2:8890/sparql-graph-crud-auth?graph-uri=$3" -T $f
done

rm cache/*
