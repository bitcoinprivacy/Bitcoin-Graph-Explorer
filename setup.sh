#! /bin/bash

add-apt-repository ppa:webupd8team/java
apt-get update
apt-get install -y bitcoin-qt oracle-java7-installer
wget http://scalasbt.artifactoryonline.com/scalasbt/sbt-native-packages/org/scala-sbt/sbt//0.12.4/sbt.deb
dpkg -i sbt.deb
sbt gen-idea
