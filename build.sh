#!/bin/sh
cd `dirname $0`

SRC_DIR="$PWD/src"
LIB_DIR="$PWD/lib"
BLD_DIR="$PWD/bld"

rm -rf $BLD_DIR
mkdir  $BLD_DIR

cd $SRC_DIR
CP=`find $LIB_DIR -name '*.jar' | awk -v ORS=':' '{print}'`
FP=`find . -name '*.java'`
javac -classpath "$CP." -d $BLD_DIR $FP && echo "[OK] Build."
