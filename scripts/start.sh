#! /bin/bash
#ionice -c 3 nice 
bitcoind -timeout=100 -dbcache=100000 -rpcuser=user -rpcpassword=pass -rpcthreads=4 -daemon -txindex=1


