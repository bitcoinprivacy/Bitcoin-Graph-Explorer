#! /bin/bash

add-apt-repository ppa:webupd8team/java
add-apt-repository ppa:bitcoin/bitcoin
apt-get update
apt-get install -y bitcoind oracle-java7-installer
if [ ! -f sbt.deb ]
    then
    wget http://scalasbt.artifactoryonline.com/scalasbt/sbt-native-packages/org/scala-sbt/sbt//0.12.4/sbt.deb
else
    echo "file up to date"
fi
dpkg -i sbt.deb
