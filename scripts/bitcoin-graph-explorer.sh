#! /bin/sh

case "$1" in
      start)
            cd /home/webdeveloper/Bitcoin-Graph-Explorer
            command="sbt \"run all 270000 init\" >> output.log &"
            eval $command
            ;;
      stop)
            killall -v sbt
            ;;
      *)
            echo "Usage bitcoins start | end"
            ;;
esac
exit 0
