#!/bin/bash
blocks=$(bitcoind -rpcuser=user -rpcpassword=pass getblockcount)

for i in $(seq 0 $blocks)
do
   bitcoind -rpcuser=user -rpcpassword=pass getblockhash $i
done
