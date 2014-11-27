sed -n "1,$1p" blockchain/status.txt > blockchain/blocklist.txt 
sbt "run populate"  > /dev/null 2>&1
echo "Blocks $1 populate"
echo "Movements"
sqlite3 blockchain/movements.db "select count(*) from movements;"
echo "Addresses"
sqlite3 blockchain/movements.db "select count(*) from addresses;"
sed -n "1,$2p" blockchain/status.txt > blockchain/blocklist.txt
echo "Blocks $2 resume"
sbt "run resume"  > /dev/null 2>&1
echo "Movements"
sqlite3 blockchain/movements.db "select count(*) from movements;"
echo "Addresses"
sqlite3 blockchain/movements.db "select count(*) from addresses;"
sed -n "1,$2p" blockchain/status.txt > blockchain/blocklist.txt
echo "Blocks $2 populate"
sbt "run populate"  > /dev/null 2>&1
echo "Movements"
sqlite3 blockchain/movements.db "select count(*) from movements;"
echo "Addresses"
sqlite3 blockchain/movements.db "select count(*) from addresses;"
