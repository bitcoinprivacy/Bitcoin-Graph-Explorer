#! /bin/bash
#ionice -c 3 nice 
<<<<<<< HEAD
bitcoind -timeout=100 -dbcache=100000 -rpcuser=user -rpcpassword=pass -rpcthreads=4 -daemon -txindex=1
=======
bitcoind -timeout=10 -dbcache=100000 -rpcthreads=24 -daemon

# -txindex=1
>>>>>>> fb3347cc5cbf1a128b98789207d64c16188742a5


