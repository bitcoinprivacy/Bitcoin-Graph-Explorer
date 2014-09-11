#!/bin/bash
fromblocks=$(cat blockchain/blocklist.txt | wc -l) 
blocks=`expr $(bitcoind getblockcount) - 5`

for i in $(seq $fromblocks $blocks)
do
    bitcoind getblockhash $i >> blockchain/blocklist.txt
done
