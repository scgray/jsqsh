##### http://autoconf-archive.cryp.to/ac_check_java_home.html
#
# SYNOPSIS
#
#   AC_CHECK_JAVA_HOME
#
# DESCRIPTION
#
#   Check for Sun Java (JDK / JRE) installation, where the 'java' VM is
#   in. If found, set environment variable JAVA_HOME = Java
#   installation home, else left JAVA_HOME untouch, which in most case
#   means JAVA_HOME is empty.
#
# LAST MODIFICATION
#
#   2002-10-10
#
# COPYLEFT
#
#   Copyright (c) 2002 Gleen Salmon <gleensalmon@yahoo.com>
#
#   This program is free software; you can redistribute it and/or
#   modify it under the terms of the GNU General Public License as
#   published by the Free Software Foundation; either version 2 of the
#   License, or (at your option) any later version.
#
#   This program is distributed in the hope that it will be useful, but
#   WITHOUT ANY WARRANTY; without even the implied warranty of
#   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
#   General Public License for more details.
#
#   You should have received a copy of the GNU General Public License
#   along with this program; if not, write to the Free Software
#   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
#   02111-1307, USA.
#
#   As a special exception, the respective Autoconf Macro's copyright
#   owner gives unlimited permission to copy, distribute and modify the
#   configure scripts that are the output of Autoconf when processing
#   the Macro. You need not follow the terms of the GNU General Public
#   License when using or distributing such scripts, even though
#   portions of the text of the Macro appear in them. The GNU General
#   Public License (GPL) does govern all other use of the material that
#   constitutes the Autoconf Macro.
#
#   This special exception to the GPL applies to versions of the
#   Autoconf Macro released by the Autoconf Macro Archive. When you
#   make and distribute a modified version of the Autoconf Macro, you
#   may extend this special exception to the GPL to apply to your
#   modified version as well.

AC_DEFUN([AC_CHECK_JAVA_HOME],[
AC_REQUIRE([AC_EXEEXT])dnl
TRY_JAVA_HOME=`ls -dr /usr/java/* 2> /dev/null | head -n 1`
if test x$TRY_JAVA_HOME != x; then
	PATH=$PATH:$TRY_JAVA_HOME/bin
fi
AC_PATH_PROG(JAVA_PATH_NAME, java$EXEEXT)
if test x$JAVA_PATH_NAME != x; then
	JAVA_HOME=`echo $JAVA_PATH_NAME | sed "s/\(.*\)[[/]]bin[[/]]java$EXEEXT$/\1/"`
fi;dnl
])


##### http://autoconf-archive.cryp.to/ac_jni_include_dir.html
#
# SYNOPSIS
#
#   AC_JAVA_JNI
#
# DESCRIPTION
#
# LAST MODIFICATION
#
#   2006-05-27
#
# COPYLEFT
#
#   Copyright (c) 2007 Scott Gray <sgray@inventa.com
#
#   Copying and distribution of this file, with or without
#   modification, are permitted in any medium without royalty provided
#   the copyright notice and this notice are preserved.

AC_DEFUN([AC_JAVA_JNI], [

   AC_MSG_CHECKING([JAVA_HOME location])

      if test "x$JAVA_HOME" = "x"; then
         AC_MSG_RESULT(no)
         AC_MSG_ERROR(['\$JAVA_HOME' is undefined. Ensure that this variable is set to the root of your java installation and re-run configure])
      fi
      if test ! -d "$JAVA_HOME"; then
         AC_MSG_RESULT(no)
         AC_MSG_ERROR([Directory specified by \$JAVA_HOME ($JAVA_HOME) does not exist])
      fi

   AC_MSG_RESULT([$JAVA_HOME])

   AC_MSG_CHECKING([Java includes])

     JNI_INC=""
     for dir in include Headers; do
        if test -d "$JAVA_HOME/$dir"; then
           JNI_INC="$dir"
           break
        fi
     done

     if test "x$JNI_INC" = "x"; then
        AC_MSG_RESULT(no)
        AC_MSG_ERROR([Unable to locate header directory under \$JAVA_HOME])
     fi

     if ! test -f "$JAVA_HOME/$JNI_INC/jni.h"; then
        AC_MSG_RESULT(no)
        AC_MSG_ERROR([Unable to locate jni.h in $JAVA_HOME/$JNI_INC])
     fi

     JAVA_INCDIR="-I\"\$(JAVA_HOME)/$JNI_INC\""

     for subdir in bsdos linux genunix alpha solaris win32; do
        if test -d "$JAVA_HOME/$JNI_INC/$subdir"; then
           JAVA_INCDIR="$JAVA_INCDIR -I\"\$(JAVA_HOME)/$JNI_INC/$subdir\""
        fi
     done

   AC_MSG_RESULT([$JAVA_INCDIR])

   AC_SUBST([JAVA_HOME])
   AC_SUBST([JAVA_INCDIR])
])
