## Variable

  `show_exclass` - Controls whether or not the exception class name is displayed

## Description

  The `${show_exclass}` boolean variable controls whether or not exceptions
  thrown during the execution of a command (such as `\go`) will display
  the class name of the exception, along with the text of the exception. The
  default is `false`. This is typically only set to true if you need to 
  debug a problem within a driver or possibly a specific command.
   
  Currently not all places that deal with exceptions in jsqsh honor this
  setting.
   
## See also

  [[show_stack]]
