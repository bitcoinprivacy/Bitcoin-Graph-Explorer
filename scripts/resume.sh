#! /bin/bash

exit
echo "Obtaining block list"
scripts/getblocklist.sh
echo "Processing blockchain"
sbt "run resume"
