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
check_include_files(string.h    HAVE_STRING_H)
check_include_files(sys/ioctl.h HAVE_SYS_IOCTL_H)
check_include_files(sys/stat.h  HAVE_SYS_STAT_H)
check_include_files(sys/types.h HAVE_SYS_TYPES_H)
check_include_files(sys/wait.h  HAVE_SYS_WAIT_H)
check_include_files(termios.h   HAVE_TERMIOS_H)
check_include_files(time.h      HAVE_TIME_H)
check_include_files(unistd.h    HAVE_UNISTD_H)
check_include_files(jni.h       HAVE_JNI_H)

include(CheckFunctionExists)
check_function_exists(localtime   HAVE_LOCALTIME)
check_function_exists(localtime_r HAVE_LOCALTIME_R)
check_function_exists(strerror    HAVE_STRERROR)
check_function_exists(strerror_r  HAVE_STRERROR_R)

if (NOT HAVE_JNI_H)
    if (DEFINED ENV{JAVA_HOME})
        if (NOT EXISTS "$ENV{JAVA_HOME}/include")
            message(FATAL_ERROR "Cannot find $ENV{JAVA_HOME}/include")
        endif()

        include_directories("$ENV{JAVA_HOME}/include")
        file(GLOB JAVA_JNI_MD "$ENV{JAVA_HOME}/include/*/jni_md.h")
        if (JAVA_JNI_MD)
            get_filename_component(JAVA_JNI_MD_DIR ${JAVA_JNI_MD} PATH)
            include_directories(${JAVA_JNI_MD_DIR})
        else()
            message(FATAL_ERROR 
                "Unable to find jni_md.h under $ENV{JAVA_HOME}/include")
        endif()
    else ()
        message(FATAL_ERROR "You have to have JAVA_HOME set")
    endif()
endif()

configure_file(${CMAKE_CURRENT_SOURCE_DIR}/config.h.cmake
    ${CMAKE_CURRENT_SOURCE_DIR}/config.h)

add_library(jsqsh SHARED
    config.h
    jsqsh.h
    jsqsh_init.c
    jsqsh_jni.c
    jsqsh_unix.c
    jsqsh_util.c
    jsqsh_win32.c
)

