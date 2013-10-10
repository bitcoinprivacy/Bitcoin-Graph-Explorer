#! /bin/bash

bitcoind -timeout=1 -dbcache=100 --txindex=1 -reindex -rpcuser=user -rpcpassword=pass -rpcthreads=8 -daemon


