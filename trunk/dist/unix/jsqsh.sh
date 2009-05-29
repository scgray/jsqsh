#!/bin/sh
#
# jsqsh - This shell script attempts to launch org.sqsh.JSqsh. 
#   it does its best to try to figure out where various files
#   reside (shared libraries, jar's, etc) across multiple 
#   distributions of Linux (Ubundu, Fedora, RedHad ES).
#

#
# This determines the location of this shell script.
#
prog=$0
if [ ! -f "$prog" ]; then
   prog=`command -v "$0"`
fi
PROG_DIR=`dirname "$prog"`
PROG_DIR=`cd "$PROG_DIR" && pwd`

#
# These are the places that we will look for our JNI files that
# we need to run (well, don't need, but are useful).
#
RL_JNI="/usr/lib/jni /usr/lib /usr/lib64 /usr/lib/libreadline-java"

#
# If there is a ../lib relative to this script directory, then
# check there.
#
if [ -d "${PROG_DIR}/../lib" ]; then
   D=`cd "${PROG_DIR}/../lib"; pwd`
   RL_JNI="$D $RL_JNI"
fi

#
# Ditto for ../lib/jni
#
if [ -d "${PROG_DIR}/../lib/jni" ]; then
   D=`cd "${PROG_DIR}/../lib/jni"; pwd`
   RL_JNI="$D $RL_JNI"
fi

#
# The debian distributions like to put the JNI DLL's
# under /usr/lib/jni
#
for dir in $RL_JNI; do
    READLINE_FILES=`ls $dir/libJavaReadline*.so 2>/dev/null`
    if [ "$READLINE_FILES" != "" ]; then
       LD_LIBRARY_PATH="$dir:$LD_LIBRARY_PATH"
    else
       EDITLINE_FILES=`ls $dir/libJavaEditline*.so 2>/dev/null`
       if [ "$EDITLINE_FILES" != "" ]; then
          LD_LIBRARY_PATH="$dir:$LD_LIBRARY_PATH"
       fi
    fi
    JSQSH_FILES=`ls $dir/libjsqsh*.so 2>/dev/null`
    if [ "$JSQSH_FILES" != "" ]; then
       LD_LIBRARY_PATH="$dir:$LD_LIBRARY_PATH"
    fi
done
export LD_LIBRARY_PATH


#
# Encorporate all of the jars that typically come with 
# jsqsh in its standard installation directory.
#
for jardir in /usr/share/jsqsh "${PROG_DIR}/../share/jsqsh"; do
   if [ -d "${jardir}" ]; then
      for jar in "${jardir}"/*.jar; do
          CLASSPATH="$CLASSPATH:$jar"
      done
      break
   fi
done

#
# Next, we need to find out where the java readline 
# library is installed.
#
RL_IMPLS="/usr/lib/java /usr/share/java /usr/lib/libreadline-java"
for dir in $RL_IMPLS; do
    if [ -f "$dir/libreadline-java.jar" ]; then
        CLASSPATH="$CLASSPATH:$dir/libreadline-java.jar"
    fi
done
export CLASSPATH

# echo "LD_LIBRARY_PATH = $LD_LIBRARY_PATH"
# echo "CLASSPATH = $CLASSPATH"

java org.sqsh.JSqsh $*
