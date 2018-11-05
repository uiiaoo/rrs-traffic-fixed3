#!/bin/sh
cd `dirname $0`

SERVER_DIR="$PWD/../rcrs-server"
LOG_DIR="$SERVER_DIR/boot/logs"

LAUNCHER='rescuecore2.LaunchComponents'
SIMULATOR='traffic.Simulator'

LIB_DIR="$PWD/lib"
BLD_DIR="$PWD/bld"
CNF_DIR="$PWD/cnf"

CP=`find $LIB_DIR -name '*.jar' | awk -v ORS=':' '{print}'`
CP=$BLD_DIR:$SERVER_DIR/supplement:$CP

java -classpath $CP. -Dlog4j.log.dir=$LOG_DIR $LAUNCHER \
    $SIMULATOR -c $CNF_DIR/traffic.cfg 2>&1 | tee $LOG_DIR/traffic-out.log
