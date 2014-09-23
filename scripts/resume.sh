#! /bin/bash
echo "Obtaining block list"
echo "Processing blockchain"
sbt "run resume"
echo "updating blockcount for website"
cat /root/.bitcoin/blockchain/blocklist.txt  | wc -l > /root/bge/blockchain/count.txt
