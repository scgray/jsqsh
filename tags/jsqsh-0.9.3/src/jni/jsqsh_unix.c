/*
 * sqsh_unix.c - UNIX implementation of JNI calls
 * Copyright (C) 2007 by Scott C. Gray
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *  
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, write to the Free Software Foundation, 675 Mass Ave,
 * Cambridge, MA 02139, USA.
 */
#include "jsqsh.h"

#ifndef HAVE_WINDOWS_H

#ifdef HAVE_SYS_TYPES_H
#include <sys/types.h>
#endif
#ifdef HAVE_SYS_WAIT_H
#include <sys/wait.h>
#endif
#ifdef HAVE_STDLIB_H
#include <stdlib.h>
#endif
#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif
#ifdef HAVE_ERRNO_H
#include <errno.h>
#endif
#ifdef HAVE_TERMIOS_H
#include <termios.h>
#endif
#ifdef HAVE_SYS_IOCTL_H
#include <sys/ioctl.h>
#endif

/*
 * jsqsh_popen()
 *
 * Actual UNIX implementation of sqsh's popen() call.
 *
 *   env     - Handle to the JVM. This should only be used to throw
 *             exceptions.
 *   args    - The command to execute (null terminated array of
 *             strings).
 *   ret_pid - (output) The handle to the process that was spawned
 *   ret_fd  - (output) The handle to the file that is to be used to 
 *             wrote to the stdin of the spawned process.
 *
 * Returns 0 upon success.
 */
int jsqsh_popen(JNIEnv *env, char **args, jlong *ret_pid, jlong *ret_fd)
{
    int       pfd[2];
    pid_t     pid;
    int       fd;
    int       e;

    /*
     * Create our pipe that will be used to communicate with our
     * newly spawned process.
     */
    if (pipe(pfd) < 0)
    {
        jsqsh_throw(env, errno, NULL);
        return 1;
    }

    pid = fork();
    switch (pid)
    {
        case -1:     /* fork() failed */
            e = errno;
            close(pfd[0]);
            close(pfd[1]);
            jsqsh_throw(env, e, NULL);
            return 1;

        case 0 :    /* We are in the context of the child process */
            /*
             * Close the write side of the pipe, that is for the JVM
             * to send input to this process
             */
            close(pfd[1]);

            /*
             * Attach the reading end of the pipe to our stdin, then
             * close our end of the pipe.
             */
            if (pfd[0] != fileno(stdin))
            {
                dup2(pfd[0], fileno(stdin));
            }
            close(pfd[0]);

            /*
             * Finally we want to spawn the process that was requested.
             */
            if (execv(args[0], (const char*) args) < 0)
            {
                fprintf(stderr, "Failed to launch '%s' (errno %d)\n",
                    args[0], errno);

                exit(1);
            }
            break;

        default:   /* We are still in the JVM */
            break;
    }

    /*
     * This code is executed in the JVM.
     */
    fd = pfd[1];
    close (pfd[0]);

    *ret_pid = (jlong) pid;
    *ret_fd = (jlong) fd;

    return 0;
}

/*
 * jsqsh_exec():
 *
 * Actual UNIX implementation of sqsh's exec() call.
 *   env     - Handle to the JVM. This should only be used to throw
 *             exceptions.
 *   args    - The command to execute (null terminated array of
 *             strings).
 *   ret_pid - (output) The handle to the process that was spawned
 * Returns 0 upon success.
 */
int jsqsh_exec (JNIEnv *env, char **args, jlong *ret_pid) 
{
    pid_t     pid;

    pid = fork();
    switch (pid)
    {
        case -1:     /* fork() failed */
            jsqsh_throw(env, errno, NULL);
            return 1;

        case 0 :    /* We are in the context of the child process */
            /*
             * Finally we want to spawn the process that was requested.
             */
            if (execv(args[0], (const char**) args) < 0)
            {
                fprintf(stderr, "Failed to launch '%s' (errno %d)\n",
                    args[0], errno);

                exit(1);
            }
            break;

        default:   /* We are still in the JVM */
            break;
    }

    *ret_pid = (jlong) pid;
    return 0;
}

/*
 * jsqsh_close()
 *   env - Handle to the JVM. This should only be used to throw
 *         exceptions.
 *   jfd - File descriptor to be closed.
 * UNIX Implementation of close().
 */
void jsqsh_close(JNIEnv *env, jlong jfd)
{
    int rc;

    rc = close((int) jfd);
    if (rc < 0)
    {
        jsqsh_throw(env, errno, "java/io/IOException");
    }
}

/*
 * jsqsh_wait()
 *
 * UNIX Implementation of wait().
 *   env - Handle to the JVM. This should only be used to throw
 *         exceptions.
 *   pid - Process to wait for
 */
int jsqsh_wait (JNIEnv *env, jlong pid)
{
    pid_t rc;
    int   status;

    rc = waitpid((pid_t) pid, &status, 0);
    if (rc < 0)
    {
        jsqsh_throw(env, errno, NULL);
        return -1;
    }

    if (WIFEXITED(rc))
    {
        return WEXITSTATUS(rc);
    }

    return 1;
}

/*
 * jsqsh_write()
 *
 * UNIX Implementation of write().
 *   env - Handle to the JVM. This should only be used to throw
 *         exceptions.
 *   jfd - File descriptor we are writing to
 *   buf - The bytes we want to write.
 *   len - The number of bytes to write.
 */
void jsqsh_write (JNIEnv *env, jlong jfd, void *buf, int len)
{
    int rc;
    int fd;
    int nbytes;

    fd     = (int) jfd;
    nbytes = 0;

    while (nbytes < len)
    {
        rc = write(fd, (void *) buf, len);
        if (rc < 0) 
        {
            jsqsh_throw(env, errno, "java/io/IOException");
            return;
        }

        buf    += rc;
        nbytes += rc;
    }
}

/*
 * jsqsh_width()
 *
 * Returns the width of the console or -1 if the width cannot be
 * determined.
 *   env - Handle to the JVM. This should only be used to throw
 *         exceptions.
 */
int jsqsh_width(JNIEnv *env)
{
#ifdef TIOCGWINSZ
    struct winsize ws;
    if (ioctl(fileno(stdout), TIOCGWINSZ, &ws) != -1)
    {
        return ws.ws_col;
    }
#endif

    return -1;
}

#endif /* !HAVE_WINDOWS_H */
