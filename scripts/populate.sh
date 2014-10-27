#!/bin/sh
date
cat .bitcoin/blocklist.txt  | wc -l > blockchain/count.txt.prov
echo "0" > blockchain/count.txt
echo "Populating database"
scala -classpath "target/scala-2.11/Bitcoin Graph Explorer-assembly-2.0.jar" -DXmx=10G Explorer populate > blockchain/populate.log
echo "Parsing errors found"
grep ERROR: blockchain/populate.log > blockchain/scripts.log
sed -i 's/ERROR://g' blockchain/scripts.log
sed -i 's/)\[/)\ \[/g' blockchain/scripts.log
date
echo "Done!"
mv blockchain/count.txt.prov blockchain/count.txt

