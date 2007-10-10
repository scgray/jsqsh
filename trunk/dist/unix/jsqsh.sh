#!/bin/sh
#
# jsqsh - This shell script attempts to launch org.sqsh.JSqsh. 
#   it does its best to try to figure out where various files
#   reside (shared libraries, jar's, etc) across multiple 
#   distributions of Linux (Ubundu, Fedora, RedHad ES).
#

#
# The debian distributions like to put the JNI DLL's
# under /usr/lib/jni
#
RL_JNI="/usr/lib/jni /usr/lib /usr/lib/libreadline-java"
for dir in $RL_JNI; do
    FILES=`ls $dir/libJavaReadline*.so`
    if [ "$FILES" != "" ]; then
       LD_LIBRARY_PATH="$RL_JNI:$LD_LIBRARY_PATH"
    fi
fi
export LD_LIBRARY_PATH

#
# Encorporate all of the jars that typically come with 
# jsqsh in its standard installation directory.
#
for jar in /usr/share/jsqsh/*.jar; do
    CLASSPATH="$CLASSPATH:$jar"
done

#
# Next, we need to find out where the java readline 
# library is installed.
#
RL_IMPLS="/usr/lib/java /usr/share/java /usr/lib/libreadline-java"
for dir in $RL_IMPLS; do
    if [ -e "$dir/libreadline-java.jar" ]; then
        CLASSPATH="$CLASSPATH:$dir/libreadline-java.jar"
    fi
done
export CLASSPATH

java org.sqsh.JSqsh $*
