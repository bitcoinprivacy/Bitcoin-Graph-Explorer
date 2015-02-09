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

for pid in $(pidof -x populate.sh); do
    if [ $pid != $$ ]; then
        #echo "[$(date)] : resume.sh : Process is already running with PID $pid"                                                                               
        exit 1
    fi
done

lastBlockNumber=`expr $(sqlite3  blockchain/movements.db "select max(block_height) from blocks; ")`
newBlockNumber=`expr $(cat .bitcoin/blocklist.txt  | wc -l)`

if [ "$lastBlockNumber" -eq "$newBlockNumber" ]; then
    exit 1
fi

echo "[$(date)] Reading blockchain until $newBlockNumber"
JAVA_OPTS="-Xmx1g" scala -classpath "target/scala-2.11/Bitcoin Graph Explorer-assembly-2.0.jar" Explorer resume > blockchain/resume.log

cat blockchain/resume.log >> blockchain/history.log
echo "[$(date)] Done!"

