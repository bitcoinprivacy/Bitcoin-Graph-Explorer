sed -n "1,$1p" blockchain/status.txt > blockchain/blocklist.txt 
sbt "run populate"  > /dev/null 2>&1
echo "Blocks $1 populate"
echo "Movements"
sqlite3 blockchain/movements.db "select count(*) from movements;"
sqlite3 blockchain/movements.db "select sum(value) from movements where spent_in_transaction_hash is null"
echo "Addresses"
sqlite3 blockchain/movements.db "select count(*) from addresses;"
sqlite3 blockchain/movements.db "select sum(balance) from addresses"
sed -n "1,$2p" blockchain/status.txt > blockchain/blocklist.txt
echo "Blocks $2 resume"
sbt "run resume" > test.log
echo "Movements"
sqlite3 blockchain/movements.db "select count(*) from movements;"
sqlite3 blockchain/movements.db "select sum(value) from movements where spent_in_transaction_hash is null"

echo "Addresses"
sqlite3 blockchain/movements.db "select count(*) from addresses;"
sqlite3 blockchain/movements.db "select sum(balance) from addresses"
sed -n "1,$2p" blockchain/status.txt > blockchain/blocklist.txt
echo "Blocks $2 populate"
sbt "run populate"  > /dev/null 2>&1
echo "Movements"
sqlite3 blockchain/movements.db "select count(*) from movements;"
sqlite3 blockchain/movements.db "select sum(value) from movements where spent_in_transaction_hash is null"

echo "Addresses"
sqlite3 blockchain/movements.db "select count(*) from addresses;"
sqlite3 blockchain/movements.db "select sum(balance) from addresses"
