date
echo "Populating database"
sbt "run populate" > blockchain/populate.log
echo "Parsing errors found"
grep ERROR: blockchain/populate.log > blockchain/scripts.log
sed -i 's/ERROR://g' blockchain/scripts.log
sed -i 's/)\[/)\ \[/g' blockchain/scripts.log
echo "Done!"
date
