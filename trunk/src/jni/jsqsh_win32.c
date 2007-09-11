/*
 * sqsh_win32.c - Windows API implementation of sqsh calls.
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

#ifdef HAVE_WINDOWS_H
#include <windows.h>
#include <tchar.h>
#include <stdio.h>
#include <string.h>

/*
 * jsqsh_commandline():
 *
 * Sweet jesus I hate windows. UNIX, rather intelligently, passes
 * arguments as individual elements to programs being spawned. Windows,
 * on the other hand, passes a whole entire command line, requiring me
 * to take lots of care to make sure that windows can parse it properly.
 */
static char* jsqsh_commandline(char **argv)
{
    char *cl;
    char *arg;
    int   i;
    int   j;
    int   len;

    cl = (char*) malloc (1024 * sizeof(char));

    len = 0;
    for (i = 0; argv[i] != NULL && len < 1023; i++) 
    {
        /*
         * Put a space between arguments.
         */
        if (i > 0)
        {
            cl[len++] = ' ';
        }

        arg = argv[i];
        for (j = 0; len < 1023 && arg[j] != '\0'; ++j) 
        {
            cl[len++] = arg[j];
        }
    }

    cl[len] = '\0';
    return cl;
}

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
    SECURITY_ATTRIBUTES security;
    PROCESS_INFORMATION process; 
    STARTUPINFO         start_info;
    HANDLE              stdin_read;
    HANDLE              stdin_write;
    char                *command_line;
    int                 ok = 1;


    /*
     * Convert our nicely parsed arguments into the monstrosity 
     * that windows requires (don't forget to free this!!).
     */
    command_line = jsqsh_commandline(args);

    /*
     * Set up our security attributes
     */
    security.nLength = sizeof(SECURITY_ATTRIBUTES); 
    security.bInheritHandle = TRUE; 
    security.lpSecurityDescriptor = NULL; 

    /*
     * Create our pipe for communications.
     */
    if (!CreatePipe(&stdin_read, &stdin_write, &security, 0)) 
    {
        jsqsh_throw(env, GetLastError(), NULL);
        free(command_line);
        return 1;
    }

    /*
     * Make sure that the child process only gets the read side of
     * the pipe.
     */
    SetHandleInformation(stdin_write, HANDLE_FLAG_INHERIT, 0);
    ZeroMemory(&process, sizeof(PROCESS_INFORMATION));
    ZeroMemory(&start_info, sizeof(STARTUPINFO) );

    start_info.cb         = sizeof(STARTUPINFO); 
    start_info.hStdError  = GetStdHandle(STD_ERROR_HANDLE);
    start_info.hStdOutput = GetStdHandle(STD_OUTPUT_HANDLE);
    start_info.hStdInput  = stdin_read;
    start_info.dwFlags   |= STARTF_USESTDHANDLES;

    if (ok && CreateProcess(NULL, 
                      command_line,   // command line 
                      NULL,           // process security attributes 
                      NULL,           // primary thread security attributes 
                      TRUE,           // handles are inherited 
                      0,              // creation flags 
                      NULL,           // use parent's environment 
                      NULL,           // use parent's current directory 
                      &start_info,    // STARTUPINFO pointer 
                      &process) == FALSE)
    {
        jsqsh_throw(env, GetLastError(), NULL);
        ok = 0;
    }
    else
    {
        CloseHandle(process.hThread);
    }

    free(command_line);
    if (!ok) 
    {
        CloseHandle(stdin_read);
        CloseHandle(stdin_write);
        return 1;
    }

    *ret_pid = (jlong) process.hProcess;
    *ret_fd = (jlong) stdin_write;
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
    SECURITY_ATTRIBUTES security;
    PROCESS_INFORMATION process; 
    STARTUPINFO         start_info;
    char                *command_line;
    int                 ok = 1;


    /*
     * Convert our nicely parsed arguments into the monstrosity 
     * that windows requires (don't forget to free this!!).
     */
    command_line = jsqsh_commandline(args);

    /*
     * Set up our security attributes
     */
    security.nLength = sizeof(SECURITY_ATTRIBUTES); 
    security.bInheritHandle = TRUE; 
    security.lpSecurityDescriptor = NULL; 

    ZeroMemory(&process, sizeof(PROCESS_INFORMATION));
    ZeroMemory(&start_info, sizeof(STARTUPINFO));
    start_info.cb = sizeof(STARTUPINFO); 

    if (ok && CreateProcess(NULL, 
                      command_line,   // command line 
                      NULL,           // process security attributes 
                      NULL,           // primary thread security attributes 
                      TRUE,           // handles are inherited 
                      0,              // creation flags 
                      NULL,           // use parent's environment 
                      NULL,           // use parent's current directory 
                      &start_info,    // STARTUPINFO pointer 
                      &process) == FALSE)
    {
        jsqsh_throw(env, GetLastError(), NULL);
        ok = 0;
    }
    else 
    {
        CloseHandle(process.hThread);
    }

    free(command_line);
    *ret_pid = (jlong) process.hProcess;
    return (ok == 1 ? 0 : 1);
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
    HANDLE h = (HANDLE) jfd;
    if (!CloseHandle(h))
    {
        jsqsh_throw(env, GetLastError(), "java/io/IOException");
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
    DWORD   rc;
    HANDLE  process = (HANDLE) pid;
    DWORD   status;

    rc = WaitForSingleObject(process, INFINITE);
    if (rc == WAIT_FAILED)
    {
        jsqsh_throw(env, GetLastError(), NULL);
        return 1;
    }

    if (!GetExitCodeProcess(process, &status))
    {
        jsqsh_throw(env, GetLastError(), NULL);
        return 1;
    }

    if (!CloseHandle(process))
    {
        jsqsh_throw(env, GetLastError(), NULL);
        return 1;
    }

    return ((int) status);
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
    HANDLE fd = (HANDLE) jfd;
    DWORD  nbytes = 0;
    DWORD  nwritten;

    while (nbytes < len)
    {
        if (!WriteFile(fd, (LPCVOID) buf, (DWORD) len,
                (LPDWORD) &nwritten, NULL))
        {
            jsqsh_throw(env, GetLastError(), "java/io/IOException");
            return;
        }

        nbytes += nwritten;
        buf    += nwritten;
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
    CONSOLE_SCREEN_BUFFER_INFO console;

    if (GetConsoleScreenBufferInfo(GetStdHandle(STD_OUTPUT_HANDLE),
        &console))
    {
        return console.dwSize.X;
    }

    return -1;
}

#endif /* HAVE_WINDOWS_H */

