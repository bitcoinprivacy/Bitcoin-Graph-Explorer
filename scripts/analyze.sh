#!/bin/sh
sbt "run test-populate" > blockchain/test.log
grep ERROR: blockchain/test.log > blockchain/scripts.log
rm blockchain/test.log
sed -i 's/ERROR://g' blockchain/scripts.log 	
sed -i 's/)\[/)\ \[/g' blockchain/scripts.log
sbt "run analyze-script"
