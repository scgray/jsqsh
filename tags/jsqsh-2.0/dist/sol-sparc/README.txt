The JNI portion of jsqsh for Solaris requires the installation
of a couple of external packages that are available at 
http://www.sunfreeware.com.  Specifically you will need:

   libreadline-5.x
   libgcc-3.4.6 (or higher) or gcc-3.4.6 (or higher)
     this provides libgcc_s.so.1

you will also need to ensure that your LD_LIBRARY_PATH contains
the directory in which these libraries are installed (typically
/usr/local/lib).
