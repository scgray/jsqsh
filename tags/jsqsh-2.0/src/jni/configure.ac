#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.

AC_INIT([jsqsh.h])
AC_CONFIG_AUX_DIR(build-aux)
AM_INIT_AUTOMAKE(jsqsh, 1.0)
AC_CONFIG_SRCDIR([jsqsh.h])
AC_CONFIG_HEADER([config.h])


# Checks for programs.
AC_PROG_CC
AC_LIBTOOL_WIN32_DLL
AM_PROG_LIBTOOL
AC_PROG_INSTALL

# Check for java dependancies
AC_JAVA_JNI

# Checks for libraries.

# Checks for header files.
AC_HEADER_STDC
AC_CHECK_HEADERS(windows.h stdlib.h string.h errno.h stdarg.h time.h stdarg.h unistd.h sys/types.h sys/stat.h sys/wait.h fcntl.h termios.h sys/ioctl.h)

# Functions
AC_CHECK_FUNCS(strerror_r strerror localtime_r localtime)

AC_OUTPUT(Makefile)
