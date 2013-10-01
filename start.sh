#! /bin/bash

bitcoind -timeout=10 -dbcache=100 --txindex=1 -reindex -rpcuser=user -rpcpassword=pass -rpcthreads=4 -daemon


