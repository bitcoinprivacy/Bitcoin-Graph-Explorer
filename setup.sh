#! /bin/bash
add-apt-repository ppa:webupd8team/java
apt-get update
apt-get install -y emacs git tmux oracle-java7-installer curl
wget http://scalasbt.artifactoryonline.com/scalasbt/sbt-native-packages/org/scala-sbt/sbt//0.12.4/sbt.deb
dpkg -i sbt.deb
