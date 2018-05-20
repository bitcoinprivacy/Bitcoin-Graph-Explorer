#!/bin/bash
export VERSION=SNAPSHOT
export REPOSITORY=jorgemartinezpizarro
export DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"

mkdir -p "${DIR}/files/psql"
mkdir -p "${DIR}/files/bitcoin"
rm "${DIR}/files/psql/" -rf
rm "${DIR}/files/bitcoin/" -rf
rm "${DIR}/data/blockchain/" -rf
cd "${DIR}/docker"

docker-compose kill
docker-compose up -d

echo "Waiting for docker to be ready"

for i in $(seq 1 1000)
do
  docker exec docker_postgres_1 pg_isready -p 5433 > /dev/null 2> /dev/null
  code1=$?
  docker exec --user bitcoin docker_bitcoin_1 bitcoin-cli -regtest -rpcuser=foo -rpcpassword=bar -rpcport=18333 getnetworkinfo > /dev/null 2> /dev/null
  code2=$?
  if [ $code1 -eq 0 ] && [ $code2 -eq 0 ]
  then
    echo "Finished loading"
    sleep 1s
    exit 0
  fi
done
echo "Timeout"
exit 1
