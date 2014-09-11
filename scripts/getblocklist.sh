#!/bin/bash
fromblocks=$(cat blockchain/blocklist.txt | wc -l) 
blocks=$(bitcoind getblockcount)

for i in $(seq $fromblocks $blocks)
do
    bitcoind getblockhash $i >> blockchain/blocklist.txt
done
# TODO: We must get the directory /var/www/data from config.
cat /var/www/data/blocklist.txt  | wc -l > /var/www/data/count.txt
