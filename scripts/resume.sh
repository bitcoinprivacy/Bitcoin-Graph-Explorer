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

lastBlockNumber=`expr $(mysql movements --host=172.17.0.61 --port 3306 -u root -ptrivial -se "select max(block_height) from blocks"|cut -f1)`
newBlockNumber=`expr $(cat /root/.bitcoin/blocklist.txt  | wc -l)`
newBlockNumber=$(($newBlockNumber-1))

if [ "$lastBlockNumber" -eq "$newBlockNumber" ]; then
    exit 1
fi

echo "[$(date)] Reading blockchain from "$lastBlockNumber" in until "$newBlockNumber

JAVA_OPTS="-Xmx1g" scala -classpath "/root/bge/target/scala-2.11/Bitcoin Graph Explorer-assembly-2.0.jar" Explorer resume > /root/bge/blockchain/resume.log

cat blockchain/resume.log >> /root/bge/blockchain/history.log
echo "[$(date)] Done!"

