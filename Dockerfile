FROM java:7
RUN apt-get update && apt-get install -y wget git emacs24-nox
RUN wget https://dl.bintray.com/sbt/native-packages/sbt/0.13.8/sbt-0.13.8.tgz
RUN tar xzf sbt-0.13.8.tgz
ENV PATH=$PATH:/sbt/bin
WORKDIR /root/
RUN git clone https://github.com/stefanwouldgo/Bitcoin-Graph-Explorer.git bge
RUN git clone https://github.com/stefanwouldgo/play-bitcoinprivacy.git 
RUN ln -s play-bitcoinprivacy/playdocker/root/.emacs* .
RUN git config --global user.email "info@bitcoinprivacy.net"
RUN git config --global user.name "Jorge and Stefan"
