#! /bin/bash
apt-get install -y emacs git tmux openjdk-7-jdk
rsync http://scalasbt.artifactoryonline.com/scalasbt/sbt-native-packages/org/scala-sbt/sbt//0.12.4/sbt.deb
dpkg -i sbt.deb
