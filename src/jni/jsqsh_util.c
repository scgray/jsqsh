/*
 * jsqsh_assert.c - Functions to handle assertions in jsqsh code.
 *
 * Copyright (C) 2007, Scott C. Gray
 * All rights reserved.
 */
#include <stdio.h>
#include "jsqsh.h"

#ifdef HAVE_STDLIB_H
#include <stdlib.h>
#endif
#ifdef HAVE_TIME_H
#include <time.h>
#endif
#ifdef HAVE_STDARG_H
#include <stdarg.h>
#endif
#ifdef HAVE_STRING_H
#include <string.h>
#endif
#ifdef HAVE_ERRNO_H
#include <errno.h>
#endif 

#if defined(_WINDOWS)
/*
 * Thank you, VC++ but I know that I'm using some unsafe functions.
 */
#pragma warning(disable: 4996)
#endif

#define LOG_DEBUG    0
#define LOG_INFO     1
#define LOG_WARN     2
#define LOG_ERROR    3
#define LOG_FATAL    4

/*
 * Controls the state of debugging.
 */
int g_jsqsh_debug = 0;

/*
 * Privates
 */
static void _jsqsh_log (int, char *, va_list);

void jsqsh_assert_fail(char *expr, char *file, int line)
{
   jsqsh_fatal("%s: %d: Assertion failed: %s\n", file, line, expr);
   abort();
}

/*
 * jsqsh_throw():
 *
 * Use to throw an exception due to an operating system error
 * code return.
 */
void jsqsh_throw(JNIEnv *env, int errnum, char *clazz)
{
    jclass    exception_class;
    jmethodID constructor;
    jobject   exception;
    char     *buffer;
    jstring   message;

    buffer = (char*) malloc(512);
    ASSERT(buffer != NULL);

    /*
     * Create copy of error string, just long enough to throw it
     * into the JVM. 
     */
#ifdef HAVE_STRERROR_R
    strerror_r(errnum, buffer, 512);
#else
    strncpy(buffer, strerror(errnum), 512);
    buffer[511] = '\0';
#endif

    /*
     * Convert it to a java string.
     */
    message = (*env)->NewStringUTF(env, buffer);
    ASSERT_JNI(env, message != NULL);

    /*
     * Free up temporary space.
     */
    free(buffer);

    if (clazz == NULL)
    {
        clazz = "org/sqsh/jni/NativeShellException";
    }

    exception_class = (*env)->FindClass(env, clazz);
    ASSERT_JNI(env, exception_class != NULL);

    constructor = (*env)->GetMethodID(env, exception_class, "<init>",
        "(Ljava/lang/String;)V");
    ASSERT_JNI(env, constructor != NULL);

    exception = (*env)->NewObject(env, exception_class, constructor, message);
    ASSERT_JNI(env, exception != NULL);

    (*env)->Throw(env, (jthrowable) exception);
    (*env)->DeleteLocalRef(env, message);
}

/*
 * jsqsh_assert_jni_fail():
 *
 * This function is called when the test of ASSERT_JNI() fails.
 * Besides just aborting, it checks to see if the reason the
 * assert failed is due to a throwable coming out of the JVM
 * (e.g. an OutOfMemoryError). If it is then it adds the reason
 * to the error message.
 */
void jsqsh_assert_jni_fail(JNIEnv *env, char *expr, char *file, int line)
{
    jthrowable exception;

    /*
     * This shouldn't happen really.
     */
    if (env == NULL)
    {
       jsqsh_assert_fail(expr, file, line);
       return;
    }

    /*
     * Look up the exception. If there is none, then just handle
     * the assert as we normally would.
     */
    exception = (*env)->ExceptionOccurred(env);
    if (exception == NULL)
    {
       jsqsh_assert_fail(expr, file, line);
       return;
    }
 
    jsqsh_fatal("%s: %d: Assertion failed: %s (stack trace sent to stderr)\n",
       file, line, expr);
    (*env)->ExceptionDescribe(env);
    abort();
}

/*
 * jsqsh_getenv()
 *
 * Returns the current JNIEnv context.
 */
JNIEnv* jsqsh_getenv ()
{
    jint    retval;
    JNIEnv *env;
 
    env = NULL;
    retval = (*g_jvm)->GetEnv(g_jvm, (void**)&env, JNI_VERSION_1_4);
    ASSERT_JNI(env, retval == JNI_OK && env != NULL);
 
    return(env);
}

/*
 * jsqsh_debug():
 *
 * Log a debugging message.
 */
void jsqsh_debug (char *message, ...)
{
    va_list ap;

    va_start(ap, message);
    _jsqsh_log(LOG_DEBUG, message, ap);
    va_end(ap);
}

/*
 * jsqsh_info():
 *
 * Log a informational message.
 */
void jsqsh_info (char *message, ...)
{
    va_list ap;

    va_start(ap, message);
    _jsqsh_log(LOG_INFO, message, ap);
    va_end(ap);
}

/*
 * jsqsh_warn():
 *
 * Log a informational message.
 */
void jsqsh_warn (char *message, ...)
{
    va_list ap;

    va_start(ap, message);
    _jsqsh_log(LOG_WARN, message, ap);
    va_end(ap);
}

/*
 * jsqsh_error():
 *
 * Log a informational message.
 */
void jsqsh_error (char *message, ...)
{
    va_list ap;

    va_start(ap, message);
    _jsqsh_log(LOG_ERROR, message, ap);
    va_end(ap);
}

/*
 * jsqsh_fatal():
 *
 * Log a informational message.
 */
void jsqsh_fatal (char *message, ...)
{
    va_list ap;

    va_start(ap, message);
    _jsqsh_log(LOG_FATAL, message, ap);
    va_end(ap);
}

/*
 * jsqsh_log():
 *
 * Used to generate logging messages.
 */
static void _jsqsh_log (int severity, char *message, va_list ap)
{
   char     *buffer;
   jclass    log_class;
   jmethodID log_method;
   jstring   jstr;
   char     *method = "debug";
   JNIEnv   *env;
   int       ok = 1;
   time_t    t;
   char      date_str[25];
   char     *sev;
   struct tm *date_tm;

#ifdef HAVE_LOCALTIME_R
   struct tm date_tm_st;
#endif


   /*
   ** If we haven't been initialized, then just bail.
   */
   if (g_jvm == NULL)
   {
      vfprintf(stderr, message, ap);
      return;
   }

   env = jsqsh_getenv();

   switch (severity)
   {
      case LOG_DEBUG: method = "debug"; break;
      case LOG_INFO:  method = "info"; break;
      case LOG_ERROR: method = "error"; break;
      case LOG_WARN:  method = "warn"; break;
      case LOG_FATAL: method = "fatal"; break;
      default:
         break;
   }

   buffer = (char*)malloc(2048);
   vsprintf(buffer, message, ap);

   /*
    * Normally after each JNI call I would do an ASSERT() to ensure
    * that the object is not NULL, however the ASSERT() macro
    * itself ultimately ends up calling _jsqsh_log() (if the
    * assertion fails) which winds us up in an infinite loop up until
    * the point that the stack blows out (ask me how I know). So,
    * instead, we just fall back to printing to stderr if we can't
    * log the way we want to.
    */
   if (ok == 1 
      && (log_class = 
         (*env)->FindClass(env, "org/sqsh/jni/ShellManager")) == NULL)
   {
      ok = 0;
   }

   if (ok == 1 
      && (log_method = (*env)->GetStaticMethodID(env, log_class, method, 
            "(Ljava/lang/String;)V")) == NULL)
   {
      ok = 0;
   }

   if (ok == 1 
      && (jstr = (*env)->NewStringUTF(env, buffer)) == NULL)
   {
      ok = 0;
   }
   
   if (ok == 1)
   {
      (*env)->CallStaticVoidMethod(env, log_class, log_method, jstr);
      (*env)->DeleteLocalRef(env, (jobject)jstr);
   }

   /*
    * If we couldn't use our java logging method or if the severity
    * is sufficiently awful that we really want to make sure that it
    * goes *somewhere* then we log to stderr.
    */
   if (ok == 0 || severity == LOG_FATAL)
   {
      switch (severity)
      {
         case LOG_DEBUG: sev = "DEBUG"; break;
         case LOG_INFO:  sev = "INFO "; break;
         case LOG_ERROR: sev = "ERROR"; break;
         case LOG_WARN:  sev = "WARN "; break;
         case LOG_FATAL: sev = "FATAL"; break;
         default:
            sev = "ERROR";
            break;
      }

      t = time(NULL);

#ifdef HAVE_LOCALTIME_R
      localtime_r(&t, &date_tm_st);
      date_tm = &date_tm_st;
#else
      date_tm = localtime(&t);
#endif

      strftime(date_str, sizeof(date_str), "%Y.%m.%d %H:%M:%S:000", 
         date_tm);

      fprintf(stderr, "%s %s %s\n", sev, date_str, buffer);
   }

   free(buffer);
}
