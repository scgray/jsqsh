/*
 * sqsh_jni.c - All public JNI functions
 *
 * Copyright 2007-2012 Scott C. Gray
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include "jsqsh.h"

/*
 * This file contains all platform independant JNI functions that are
 * called by SQSH. It, in turn, calls out to the platform dependant
 * functions at certain points.
 */
#ifdef HAVE_SYS_TYPES_H
#include <sys/types.h>
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

/*
 * Shell ShellManager.popen(String []cmdArgs)
 *
 * Writes a byte to a file descriptor
 */
JNIEXPORT jobject JNICALL JSQSH_PKG(popen)
    (JNIEnv *env, jclass jcls, jobjectArray cmdArgs)
{
    jlong     pid;
    jlong     fd;
    char      **args;
    int       nargs;
    int       i;
    jstring   jstr;
    jclass    shell_class;
    jmethodID constructor;
    jobject   shell;
    int       rc;

    /*
     * Convert the arguments that were passed in into strings
     * that we can understand down here in the lowely C land.
     */
    nargs = (*env)->GetArrayLength(env, cmdArgs);
    ASSERT_JNI(env, nargs > 0);

    args = (char**) malloc(sizeof(char*) * (nargs + 1));
    ASSERT(args != NULL);

    for (i = 0; i < nargs; i++)
    {
        jstr = (*env)->GetObjectArrayElement(env, cmdArgs, i);
        ASSERT_JNI(env, jstr != NULL);

        args[i] = (char *) (*env)->GetStringUTFChars(env, jstr, NULL);
        ASSERT_JNI(env, args[i] != NULL);

        (*env)->DeleteLocalRef(env, jstr);
    }
    args[nargs] = NULL;

    rc = jsqsh_popen(env, args, &pid, &fd);

    /*
     * Before we do anything else, clean up the resources we have
     * consumed thus far.
     */
    for (i = 0; i < nargs; i++)
    {
        jstr = (*env)->GetObjectArrayElement(env, cmdArgs, i);
        ASSERT_JNI(env, jstr != NULL);

        (*env)->ReleaseStringUTFChars(env, jstr, args[i]);
        (*env)->DeleteLocalRef(env, jstr);
    }
    free(args);

    /*
     * Now, create our shell class to return to the caller.
     */
    if (rc == 0)
    {
        shell_class = (*env)->FindClass(env, "org/sqsh/jni/NativeShell");
        ASSERT_JNI(env, shell_class != NULL);

        constructor = (*env)->GetMethodID(env, shell_class, "<init>", "(JJ)V");
        ASSERT_JNI(env, constructor != NULL);

        shell = (*env)->NewObject(env, shell_class, constructor, 
            (jlong) pid, (jlong) fd);
        ASSERT_JNI(env, shell != NULL);

        return shell;
    }
    else
    {
        return NULL;
    }
}

/*
 * Shell ShellManager.exec(String []cmdArgs)
 *
 * Writes a byte to a file descriptor
 */
JNIEXPORT jobject JNICALL JSQSH_PKG(exec)
    (JNIEnv *env, jclass jcls, jobjectArray cmdArgs)
{
    jlong     pid;
    char      **args;
    int       nargs;
    int       i;
    jstring   jstr;
    jclass    shell_class;
    jmethodID constructor;
    jobject   shell;
    int       rc;

    /*
     * Convert the arguments that were passed in into strings
     * that we can understand down here in the lowely C land.
     */
    nargs = (*env)->GetArrayLength(env, cmdArgs);
    ASSERT_JNI(env, nargs > 0);

    args = (char**) malloc(sizeof(char*) * (nargs + 1));
    ASSERT(args != NULL);

    for (i = 0; i < nargs; i++)
    {
        jstr = (*env)->GetObjectArrayElement(env, cmdArgs, i);
        ASSERT_JNI(env, jstr != NULL);

        args[i] = (char *) (*env)->GetStringUTFChars(env, jstr, NULL);
        ASSERT_JNI(env, args[i] != NULL);

        (*env)->DeleteLocalRef(env, jstr);
    }
    args[nargs] = NULL;

    rc = jsqsh_exec(env, args, &pid);

    /*
     * Before we do anything else, clean up the resources we have
     * consumed thus far.
     */
    for (i = 0; i < nargs; i++)
    {
        jstr = (*env)->GetObjectArrayElement(env, cmdArgs, i);
        ASSERT_JNI(env, jstr != NULL);

        (*env)->ReleaseStringUTFChars(env, jstr, args[i]);
        (*env)->DeleteLocalRef(env, jstr);
    }
    free(args);

    if (rc == 0)
    {
        /*
         * Now, create our shell class to return to the caller.
         */
        shell_class = (*env)->FindClass(env, "org/sqsh/jni/NativeShell");
        ASSERT_JNI(env, shell_class != NULL);

        constructor = (*env)->GetMethodID(env, shell_class, "<init>", "(J)V");
        ASSERT_JNI(env, constructor != NULL);

        shell = (*env)->NewObject(env, shell_class, constructor, (jlong) pid);
        ASSERT_JNI(env, shell != NULL);
        return shell;
    }

    return NULL;
}

/*
 * ShellManager.close(fd)
 */
JNIEXPORT void JNICALL JSQSH_PKG(close)
    (JNIEnv *env, jclass jcls, jlong jfd)
{
    jsqsh_close(env, jfd);
}


/*
 * ShellManager.waitPid(long pid)
 */
JNIEXPORT int JNICALL JSQSH_PKG(waitPid)
    (JNIEnv *env, jclass jcls, jlong pid)
{
    return (jsqsh_wait(env, pid));
}


/*
 * ShellManager.writeByte(fd, int byte)
 *
 * Writes a byte to a file descriptor
 */
JNIEXPORT void JNICALL JSQSH_PKG(writeByte)
    (JNIEnv *env, jclass jcls, jlong jfd, jint byte)
{
    jsqsh_write(env, jfd, &byte, 1);
}

/*
 * ShellManager.writeBytes(fd, byte []byte, int offset, int len)
 *
 * Writes a byte to a file descriptor
 */
JNIEXPORT void JNICALL JSQSH_PKG(writeBytes)
    (JNIEnv *env, jclass jcls, jlong jfd, jbyteArray bytes,
        jint offset, jint len)
{
    jbyte *buf;

    /*
     * Get the contents of the buffer to be written.
     */
    buf = (*env)->GetByteArrayElements(env, bytes, NULL);
    ASSERT_JNI(env, buf != NULL);

    jsqsh_write(env, jfd, buf + offset, len);

    (*env)->ReleaseByteArrayElements(env, bytes, buf, 0);
}

/*
 * ShellManager.getScreenWidth()
 *
 * Writes a byte to a file descriptor
 */
JNIEXPORT jint JNICALL JSQSH_PKG(getScreenWidth) (JNIEnv *env, jclass jcls)
{
    return jsqsh_width(env);
}
