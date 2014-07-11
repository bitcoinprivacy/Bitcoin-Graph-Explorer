#! /bin/bash
#ionice -c 3 nice 
bitcoind -timeout=10 -dbcache=100000 -rpcthreads=24 -daemon

# -txindex=1


