## Variable

  `terminator` - Defines the SQL statement terminator.

## Description

  The `${terminator}` variable defines a single character that can be used
  to terminate a SQL statement or block and cause it to be executed without
  having to issue a `\go`, for example:
   
    1> select * from sysobjects;
       
  will directly execute the query because `;` is the current terminator.
   
  `\unset`'ing this variable disables the in-line query terminator feature
  and requires that you explicitly issue a `\go` to execute your queries.
   
  Be very careful choosing an alternative terminator, since you don't
  want to be inadvertently executing queries.

## Command terminator

  As of jsqsh 1.5, the terminator may also be used to terminate a line
  with a jsqsh command. For example, if `;` is the current terminator
  then you can type:
   
    1> \echo hello how are you;
    hello how are you
   
  and the terminator will be silently ignored. If any input follows 
  the terminator, an error will be issued:
   
    1> \echo hello; there
    Input is not allowed after the command terminator `;`
   
## See also

  [[\go|go]], [[\unset|unset]]
