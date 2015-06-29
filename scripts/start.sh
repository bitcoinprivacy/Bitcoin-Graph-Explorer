cd /root/bge
touch lock
echo "Starting bge"
a="JAVA_OPTS=-Xmx1g /usr/bin/scala -classpath /root/bge/target/scala-2.11/Bitcoin\ Graph\ Explorer-assembly-2.0.jar Explorer bge"
eval $a
