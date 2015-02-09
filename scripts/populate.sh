#!/bin/sh
#cat .bitcoin/blocklist.txt  | wc -l > blockchain/count.txt.prov
#echo "0" > blockchain/count.txt
echo "[$(date)] Populating database"
JAVA_OPTS="-Xmx14g" scala -classpath "target/scala-2.11/Bitcoin Graph Explorer-assembly-2.0.jar" Explorer populate > blockchain/populate.log
echo "[$(date)] Done!"
cp blockchain/populate.log blockchain/history.log
#mv blockchain/count.txt.prov blockchain/count.txt

