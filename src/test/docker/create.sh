#!/usr/bin/env bash

CMD="docker exec --user bitcoin docker_bitcoin_1 bitcoin-cli -regtest -rpcuser=foo -rpcpassword=bar -rpcport=18333 "
ADD1=$(${CMD} getnewaddress)
X=$(${CMD} sendtoaddress ${ADD1} $1)
