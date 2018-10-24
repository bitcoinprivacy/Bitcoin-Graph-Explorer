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
