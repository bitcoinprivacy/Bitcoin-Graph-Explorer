#! /bin/bash
cd /root/bge
cat /root/.bitcoin/blocklist.txt  | wc -l > /root/bge/blockchain/count.txt.prov
echo "Processing blockchain"
sbt "run resume"
echo "updating blockcount for website"
mv blockchain/count.txt.prov blockchain/count.txt
