rm /root/files-regtest/bitcoin/regtest/ -rf
rm /root/Bitcoin-Graph-Explorer/src/test/blockchain/ -rf
cd /root/regtest-bge
make stop start
sleep 5s
