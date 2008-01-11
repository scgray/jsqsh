#!/bin/sh

JDK="/cygdrive/c/Program Files/Java/jdk1.5.0_12"

cp config.h.win32 config.h

gcc -mno-cygwin -I"$JDK/include" -I"$JDK/include/win32" \
    -Wl,--add-stdcall-alias -shared  \
    -DHAVE_CONFIG_H \
    -o jsqsh.dll *.c
