#! /bin/bash
echo "Obtaining block list"
scripts/getblocklist.sh
echo "Processing blockchain"
sbt "run resume"
echo "updating blockcount for website"
cat /root/bge/blockchain/blocklist.txt  | wc -l > /root/bge/blockchain/count.txt
