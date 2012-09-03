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
PROG_DIR=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

#
# The parent directory of where this script was found
#
ROOT_DIR=$(dirname "$PROG_DIR")

#
# These are the places that we will look for our JNI files that
# we need to run (well, don't need, but are useful). This path
# is designed to work if jsqsh is packaged in a self contained
# directory structure or is installed within the operating system's
# expected directory structure.
#
RL_JNI="${ROOT_DIR}/lib ${ROOT_DIR}/lib/jni /usr/lib/jni /usr/lib /usr/lib64 /usr/lib/libreadline-java"

#
# The debian distributions like to put the JNI DLL's
# under /usr/lib/jni
#
for dir in $RL_JNI; do
    is_added=0
    READLINE_FILES=$(ls $dir/libJavaReadline*.so 2>/dev/null)
    if [ "$READLINE_FILES" != "" ]; then
       LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$dir"
       is_added=1
    else
       EDITLINE_FILES=$(ls $dir/libJavaEditline*.so 2>/dev/null)
       if [ "$EDITLINE_FILES" != "" ]; then
          LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$dir"
          is_added=1
       fi
    fi

    JSQSH_FILES=$(ls $dir/libjsqsh*.so 2>/dev/null)
    if [ $is_added -eq 0 -a "$JSQSH_FILES" != "" ]; then
       LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$dir"
    fi
done
export LD_LIBRARY_PATH


#
# Encorporate all of the jars that typically come with 
# jsqsh in its standard installation directory.
#
for jardir in "${ROOT_DIR}/share" /usr/share/jsqsh "${PROG_DIR}/../share/jsqsh"; do
   if [ -d "${jardir}" -a -f "${jardir}/jsqsh.jar" ]; then
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

#
# The user is trying to run a Jaql session if $JAQL_HOME is set
# and points to a valid directory and they have specified a --jaql
# option.  
#
has_jaql=0
if [ ! -z "$JAQL_HOME" -a -d "$JAQL_HOME/bin" ]; then
    for opt in $*; do
        long_name=$(echo "$opt" | cut -c1-6)
        short_name=$(echo "$opt" | cut -c1-2)
        if [ "$long_name" = "--jaql" \
               -o "$short_name" = "-j" \
               -o "$short_name" = "-J" \
               -o "$short_name" = "-p" ]; then
            has_jaql=1
        fi
    done
fi

# echo "LD_LIBRARY_PATH = $LD_LIBRARY_PATH"
# echo "CLASSPATH = $CLASSPATH"

#
# This is for jaql support in jsqsh. If $JAQL_HOME is defined and there
# is a 'jaql' launch script in there, then use it to launch Jsqsh. This
# will ensure that the classpath is all good to run jsqsh. As far as I
# know this configuration will only work on BigInsights. 
#
if [ $has_jaql -eq 1 ]; then
   export HADOOP_CLASSPATH=$CLASSPATH
   exec $JAQL_HOME/bin/jaql "-Djava.library.path=$LD_LIBRARY_PATH" org.sqsh.JSqsh $*
else
   exec java org.sqsh.JSqsh $*
fi
