## Variable

  `width` - The screen display width

## Description

  The `${width}` variable controls the width of the screen that jsqsh uses
  when formatting output. By default, this variable is derived by directly
  polling your terminal and, thus, the value can change if your terminal
  is resized, however setting the variable to an explicit value fixes the
  display width to the size requested.  Setting the variable to a value
  less than zero, causes the width of the terminal to be utilized again.
   
## See also

  [[style]]
