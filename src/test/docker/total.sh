#!/usr/bin/env bash
CMD="docker exec --user bitcoin docker_bitcoin_1 bitcoin-cli -regtest -rpcuser=foo -rpcpassword=bar -rpcport=18333 "
X=$(${CMD} getbalance)
echo $X
