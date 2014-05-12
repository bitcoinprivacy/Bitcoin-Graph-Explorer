#! /bin/bash
bitcoind -timeout=30000 -dbcache=1000 -rpcuser=user -rpcpassword=pass -rpcthreads=1 -daemon -txindex=1


