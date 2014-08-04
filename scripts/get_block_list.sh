#!/bin/bash
fromblocks=$(cat blockchain/blocklist.txt | wc -l) 
blocks=$(bitcoind getblockcount)

for i in $(seq $fromblocks $blocks)
do
    bitcoind getblockhash $i >> blockchain/blocklist.txt
done 
