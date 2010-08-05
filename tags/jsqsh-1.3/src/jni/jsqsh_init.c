#include "jsqsh.h"
#ifdef HAVE_SYS_TYPES_H
#include <sys/types.h>
#endif
#ifdef HAVE_SYS_STAT_H
#include <sys/stat.h>
#endif
#ifdef HAVE_FCNTL_H
#include <fcntl.h>
#endif

/*
 * Private declarations.
 */
static void set_int_field
   (JNIEnv *env, jclass clazz, char *constant, int value);

/*
 * This is determined by init() and is the handle back to the JVM.
 */
JavaVM *g_jvm = NULL;

/*
 * JSqsh.init()
 * 
 * JNI implementation of init().
 */ 
JNIEXPORT void JNICALL JSQSH_PKG(init)(JNIEnv *env, jclass cls)
{
    jint   retval;
    jclass clazz;

    retval = (*env)->GetJavaVM(env, &g_jvm); 
    ASSERT_JNI(env, retval == JNI_OK && g_jvm != NULL);

    /*
     * This populates constants that may have operating system
     * specific values.
     */
    clazz = (*env)->FindClass(env, "org/sqsh/jni/ShellManager");
    ASSERT_JNI(env, clazz != NULL);

    /*
     * Constants that are required for open().
     */
    /*
    set_int_field(env, clazz, "O_RDONLY", O_RDONLY);
    set_int_field(env, clazz, "O_WRONLY", O_WRONLY);
    set_int_field(env, clazz, "O_RDWR",   O_RDWR);
    set_int_field(env, clazz, "O_APPEND", O_APPEND);
    set_int_field(env, clazz, "O_CREAT",  O_CREAT);
    set_int_field(env, clazz, "O_TRUNC",  O_TRUNC);
    */
}

/*
 * set_int_field():
 *
 * Used to set an integer field of a class.
 */
static void set_int_field(JNIEnv *env, jclass clazz, char *constant, int value)
{
    jfieldID field;

    field = (*env)->GetStaticFieldID(env, clazz, constant, "I");
    ASSERT_JNI(env, field != NULL);

    (*env)->SetStaticIntField(env, clazz, field, (jint)value);
}
