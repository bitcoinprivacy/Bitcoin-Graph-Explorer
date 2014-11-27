#! /bin/bash
cd /root/bge
# crontab needs to change directory to the bge repository folder
for pid in $(pidof -x resume.sh); do
    if [ $pid != $$ ]; then
        #echo "[$(date)] : resume.sh : Process is already running with PID $pid"
        exit 1
    fi
done
echo "[$(date)] Reading blockchain"
cat .bitcoin/blocklist.txt  | wc -l > blockchain/count.txt.prov
JAVA_OPTS="-Xmx1g" scala -classpath "target/scala-2.11/Bitcoin Graph Explorer-assembly-2.0.jar" Explorer resume > blockchain/resume.log
grep ERROR: blockchain/resume.log >> blockchain/scripts.log
sed -i 's/ERROR://g' blockchain/scripts.log
sed -i 's/)\[/)\ \[/g' blockchain/scripts.log
mv blockchain/count.txt.prov blockchain/count.txt
cat blockchain/resume.log >> blockchain/history.log
echo "[$(date)] Done!"

