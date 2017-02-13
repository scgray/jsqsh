## Variable

  `histsize` - Controls the number of SQL statements retained in history

## Description

  The `${histsize}` variable determines the number of SQL statements
  that is retained historically (the default is 50). Each time a 
  SQL statement is executed with the `\go` command, that statement
  is saved away and be re-edited and re-executed later if desired.
   
  The `\history` command shows the last `${histsize}` SQL statements
  executed and `\buf-edit` can be used to edit and re-run a 
  previously executed statement. Also, see **[[buffers]]** for
  details on how to refer to specific buffers.
   
## See also

  [[buffers]], [[\history|history]]
