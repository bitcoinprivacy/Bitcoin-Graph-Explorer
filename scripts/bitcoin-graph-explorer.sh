#! /bin/sh

case "$1" in
      start)
            cd /home/btc/Bitcoin-Graph-Explorer
            command="ionice -c 3 nice sbt \"run all 300000 init\" >> output.log &"
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
