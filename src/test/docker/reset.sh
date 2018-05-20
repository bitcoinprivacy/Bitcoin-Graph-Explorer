rm /root/Bitcoin-Graph-Explorer/src/test/data/bitcoin/regtest/ -rf
rm /root/Bitcoin-Graph-Explorer/src/test/data/blockchain/ -rf
cd /root/Bitcoin-Graph-Explorer/src/test/docker
make stop start
echo "waiting for components to be ready"
echo "5"
sleep 1s
echo "4"
sleep 1s
echo "3"
sleep 1s
echo "2"
sleep 1s
echo "1"
sleep 1s
echo "done sleeping"
