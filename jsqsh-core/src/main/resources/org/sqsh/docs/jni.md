## Jsqsh JNI layer

JSqsh is written in almost 100% java code, making it very 
portable between platforms, however there are a few places 
where java just does not provide enough functionality to 
implement certain features that are needed by jsqsh, so jsqsh 
can optinally load  a shared library (or DLL) that provides
this additional  functionality. This shared library is
written using the Java Native Interface (JNI) and on UNIX
platforms is typically called 'libjsqsh.so' and on
Windows is 'libjsqsh.dll'.
            
The jsqsh shared library is not available on all platforms, and
jsqsh will generate a warning upon startup if it is unable to
load its JNI interface.  Lack of JNI causes the following
behavior:
            
* Operating system commands that are piped to (e.g. `go | grep ...`)
  will not have direct access to the console (in the UNIX world
  this is called "tty" access). This means that these commands 
  cannot perform any console operations. Specifically, commands
  such as "more" will not function properly because "more" needs
  to query the console as to its size and will be unable to do so.
          
* Similarly, the editor launched by `\buf-edit` will not have access
  to the console, so text-based editors such as `vi` or `emacs` 
  will not work properly. In this case, you will need to use a 
  graphical editor such as "notepad.exe" or "gvim".
