#! /bin/bash

bitcoind -timeout=10 -dbcache=100 -rpcuser=user -rpcpassword=pass -rpcthreads=4 -daemon

