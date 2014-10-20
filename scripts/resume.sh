#! /bin/bash
for pid in $(pidof -x resume.sh); do
    if [ $pid != $$ ]; then
        echo "[$(date)] : resume.sh : Process is already running with PID $pid"
        exit 1
    fi
done
echo "[$(date)] Reading blocklist"
cd /root/bge
cat /root/.bitcoin/blocklist.txt  | wc -l > /root/bge/blockchain/count.txt.prov
echo "Processing blockchain"
sbt "run resume" > blockchain/resume.log
echo "Parsing errors found" 
grep ERROR: blockchain/resume.log >> blockchain/scripts.log
sed -i 's/ERROR://g' blockchain/scripts.log
sed -i 's/)\[/)\ \[/g' blockchain/scripts.log
mv blockchain/count.txt.prov blockchain/count.txt
echo "[$(date)]<b>Done!</b>"

