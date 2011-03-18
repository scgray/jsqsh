cmake_minimum_required (VERSION 2.6)

include(CheckIncludeFiles)
check_include_files(dlfcn.h     HAVE_DLFCN_H)
check_include_files(errno.h     HAVE_ERRNO_H)
check_include_files(fcntl.h     HAVE_FCNTL_H)
check_include_files(inttypes.h  HAVE_INTTYPES_H)
check_include_files(windows.h   HAVE_WINDOWS_H)
check_include_files(memory.h    HAVE_MEMORY_H)
check_include_files(stdarg.h    HAVE_STDARG_H)
check_include_files(stdint.h    HAVE_STDINT_H)
check_include_files(stdlib.h    HAVE_STDLIB_H)
check_include_files(strings.h   HAVE_STRINGS_H)
check_include_files(sys/ioctl.h HAVE_SYS_IOCTL_H)
check_include_files(sys/stat.h  HAVE_SYS_STAT_H)
check_include_files(sys/types.h HAVE_SYS_TYPES_H)
check_include_files(sys/wait.h  HAVE_SYS_WAIT_H)
check_include_files(termios.h   HAVE_TERMIOS_H)
check_include_files(time.h      HAVE_TIME_H)
check_include_files(unistd.h    HAVE_UNISTD_H)

include(CheckFunctionExists)
check_function_exists(localtime   HAVE_LOCALTIME)
check_function_exists(localtime_r HAVE_LOCALTIME_R)
check_function_exists(strerror    HAVE_STRERROR)
check_function_exists(strerror_r  HAVE_STRERROR_R)

configure_file(${CMAKE_CURRENT_SOURCE_DIR}/config.h.cmake
    ${CMAKE_CURRENT_SOURCE_DIR}/config.h)

add_library(jsqsh SHARED
    jsqsh.def
    config.h
    jsqsh.h
    jsqsh_init.c
    jsqsh_jni.c
    jsqsh_unix.c
    jsqsh_util.c
    jsqsh_win32.c
)

