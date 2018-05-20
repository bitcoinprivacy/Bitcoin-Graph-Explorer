#!/bin/bash
export VERSION=SNAPSHOT
export REPOSITORY=jorgemartinezpizarro
export DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
echo $DIR

mkdir -p "${DIR}/data/psql"
mkdir -p "${DIR}/data/bitcoin"
rm "${DIR}/data/bitcoin/" -rf
rm "${DIR}/data/blockchain/" -rf
cd "${DIR}/docker"
docker-compose kill
docker-compose up -d
## Wait until both docker images are ready
for i in $(seq 1 25)
do
  docker exec -it docker_postgres_1 pg_isready -p 5433 > /dev/null 2> /dev/null
  rc1=$?
  docker exec --user bitcoin docker_bitcoin_1 bitcoin-cli -regtest -rpcuser=foo -rpcpassword=bar -rpcport=18333 getnetworkinfo > /dev/null 2> /dev/null
  rc2=$?
  if [ $rc1 -eq 0 ] && [ $rc2 -eq 0 ]
  then
    echo "start"
    exit 0
  else 
    echo "waiting"
  fi
done
echo "Timeout"
exit 1
