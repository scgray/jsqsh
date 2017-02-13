## Variable

  `shell` - Defines the O/S shell that is used to execute commands

## Description

  The `${shell}` variable defines the external operating system program (shell)
  that will be used when executing external commands. This variable is
  used when:
   
  * Executing a pipe (e.g. **go | more**)
  * Utilizing a back-tick (e.g. **\echo \`echo hi\`**)
  * Running external editors (e.g. by **\buf-edit**).
   
The variable must contain the name of the shell, plus a comma-delimited
  list of arguments to pass to the shell. A special argument of `?` must
  be provided to indicate where the command being executed should be 
  placed.
   
  On UNIX platforms, `${shell}` will be defaulted to:
   
    /bin/sh,-c,?
       
  which means, for example, when running `go | more`, the `more` command
  will be launched via:
   
    /bin/sh -c "more"
      
  On Windows platforms `${shell}` will be defaulted to:
   
    cmd.exe,/c,?
      
## See also

  [[\buf-edit|\buf_edit|buf-edit|\buf_edit]]
