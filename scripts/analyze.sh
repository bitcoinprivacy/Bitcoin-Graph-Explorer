sbt "run populate" > blockchain/populate.log
grep ERROR: blockchain/populate.log > blockchain/scripts.log
sed -i 's/ERROR://g' blockchain/scripts.log 	
sed -i 's/)\[/)\ \[/g' blockchain/scripts.log
sbt "run analyze"
