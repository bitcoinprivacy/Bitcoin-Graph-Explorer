#! /bin/bash
bitcoind -timeout=50 -dbcache=1000 --txindex=1 -reindex -rpcuser=user -rpcpassword=pass -rpcthreads=8 -daemon


