#!/bin/sh
date
cat .bitcoin/blocklist.txt  | wc -l > blockchain/count.txt.prov
echo "0" > blockchain/count.txt
echo "[$(date)] Populating database"
JAVA_OPTS="-Xmx12g" scala -classpath "target/scala-2.11/Bitcoin Graph Explorer-assembly-2.0.jar" Explorer populate > blockchain/populate.log
echo "Parsing errors found"
grep ERROR: blockchain/populate.log > blockchain/scripts.log
sed -i 's/ERROR://g' blockchain/scripts.log
sed -i 's/)\[/)\ \[/g' blockchain/scripts.log
echo "[$(date)] Done!"
mv blockchain/count.txt.prov blockchain/count.txt

