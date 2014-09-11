#! /bin/bash
echo "Obtaining block list"
scripts/getblocklist.sh
echo "Processing blockchain"
sbt "run resume"
echo "updating blockcount for website"
scripts/updatecount.sh
