mkdir -p /root/Bitcoin-Graph-Explorer/src/test/data/psql
mkdir -p /root/Bitcoin-Graph-Explorer/src/test/data/bitcoin
rm /root/Bitcoin-Graph-Explorer/src/test/data/bitcoin/ -rf
rm /root/Bitcoin-Graph-Explorer/src/test/data/blockchain/ -rf
cd /root/Bitcoin-Graph-Explorer/src/test/docker
make stop start
echo "waiting for components to be ready"
sleep 5s
