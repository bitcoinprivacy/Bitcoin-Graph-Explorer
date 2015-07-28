#!/bin/sh

echo "[$(date)] Populating database"
JAVA_OPTS="-Xmx11g" scala -classpath "target/scala-2.11/Bitcoin Graph Explorer-assembly-2.0.jar" Explorer populate > blockchain/populate.log
echo "[$(date)] Done!"
cp blockchain/populate.log blockchain/history.log
