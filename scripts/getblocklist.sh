#!/bin/bash
fromblocks=$(cat ../.bitcoin/blocklist.txt | wc -l)
blocks=`expr $(bitcoin-cli getblockcount) - 5`

for i in $(seq $fromblocks $blocks)
do
    bitcoin-cli getblockhash $i >> ../.bitcoin/blocklist.txt
done
