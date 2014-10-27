#! /bin/bash
# crontab need to change directory to the bge repository folder
date
echo "Reading blocklist"
cat .bitcoin/blocklist.txt  | wc -l > blockchain/count.txt.prov
echo "Processing blockchain"
scala -classpath "target/scala-2.11/Bitcoin Graph Explorer-assembly-2.0.jar" -DXmx=1G Explorer resume > blockchain/resume.log
sbt "run resume" > blockchain/resume.log
echo "Parsing errors found" 
grep ERROR: blockchain/resume.log >> blockchain/scripts.log
sed -i 's/ERROR://g' blockchain/scripts.log
sed -i 's/)\[/)\ \[/g' blockchain/scripts.log
mv blockchain/count.txt.prov blockchain/count.txt
date
echo "Done!"

