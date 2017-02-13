## Variable

  `readline` - Displays the readline implementation in use.

## Description

  The read-only `${readline}` variable displays the mechanism that is
  being used to read input. 
   
  JSqsh is written in Java and java provides no easy mechanism to 
  work with the console for activities such as editing the current 
  line, or scrolling forward and backward through history or performing
  command-line completion.  As a result, JSqsh relies upon some 3rd
  party libraries to assist in these activities, some of which are
  actual native code rather than Java code. 
   
  Upon start up, JSqsh goes through some logic to determine which
  libraries are available on your platform and it chooses the first
  available that it can successfully utilize in the following order:
   
  * `jline` - JLine is an almost-pure java API that provides much of the 
    functionality of GNU readline. It is the default editor for jsqsh.
    Documentation is available at:  
    https://github.com/jline/jline2
  * `readline` - This is an interface to the GNU readline API. This
    library provides full command line editing and completion
    plus has configurable key-bindings and modes for those 
    that prefer different configurations. Documentation
    for configuring is available from:  
    http://tiswww.case.edu/php/chet/readline/rltop.html
  * `editline` - This is an interface to the NetBSD editline library. This 
    library is very similar in functionality to GNU Readline. Documentation 
    for configuring is available from:  
    http://linux.die.net/man/5/editrc
  * `getline` - Getline is used primarily on Windows and provides basic 
    command line editing, but no completion functionality.
  * `purejava` - This is the most basic interface and provides no editing 
    or completion functionality.
   
Although it is not possible to change the readline library at run-time,
  you may force the library to be used with with the `--readline` command
  line option to jsqsh or by setting the `JSQSH_READLINE` environment variable.
  Both methods may be provided one of the names above.
              
## See also

  [[options]]
