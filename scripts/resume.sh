#! /bin/bash
cd /root/bge
#
#blocks=`expr $(cat ../.bitcoin/blocklist.txt | wc -l)`
#blocks=$((blocks-5))
#echo $blocks

# crontab needs to change directory to the bge repository folder
for pid in $(pidof -x resume.sh); do
    if [ $pid != $$ ]; then
        #echo "[$(date)] : resume.sh : Process is already running with PID $pid"
        exit 1
    fi
done
cat .bitcoin/blocklist.txt  | wc -l > blockchain/count.txt.prov
blocksnummer=`expr $(cat blockchain/count.txt.prov)`
echo "[$(date)] Reading blockchain until $blocksnummer"
JAVA_OPTS="-Xmx1g" scala -classpath "target/scala-2.11/Bitcoin Graph Explorer-assembly-2.0.jar" Explorer resume > blockchain/resume.log
mv blockchain/count.txt.prov blockchain/count.txt
cat blockchain/resume.log >> blockchain/history.log
echo "[$(date)] Done!"

