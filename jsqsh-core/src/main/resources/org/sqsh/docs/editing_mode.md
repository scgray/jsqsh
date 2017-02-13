## Variable

  `editing_mode` - Controls the editing mode for the line editor.

## Description

  The `${editing_mode}` variable can be used to set or display the editing
  mode of the underlying line editing implementing, for example to switch
  to `emacs` or `vi` key sets.  Currently only the JLine editor supports
  this feature, all others will display this variable as null and will
  error when attempting to set this variable.
   
  JLine supports the following modes:
   
  * `vi` - The vi key set, starting out in insertion mode
  * `vi-insert` - The vi key set, starting out in insertion mode
  * `vi-move` - The vi key set, starting out in move/command mode
  * `emacs` - The emacs key set, starting out in insertion mode
