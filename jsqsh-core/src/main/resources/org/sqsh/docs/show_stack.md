## Variable

  `show_stack` - Controls whether or not stack traces are display from exceptions

## Description

  Note: this variable has been depricated. You should use `${ex_detail}` now
  to control the level of detail in exceptions.
   
  The `${show_stack}` boolean variable controls whether or not exceptions
  thrown during the execution of a command (such as `\go`) will display
  just the text of the exception or the full stack trace.  The default 
  is `false`, limiting the output to just the text of the exception.
  This is typically only set to true if you need to debug a problem within
  a driver or possibly a specific command.
   
  Currently not all places that deal with exceptions in jsqsh honor this
  setting.
   
## See also
  show_exclass
