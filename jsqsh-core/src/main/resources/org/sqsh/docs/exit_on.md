## Variable

  `exit_on` - List of commands that, if they error, will exit the current session

## Description

  The `${exit_on}` variable may contain a comma delimited list of commands that,
  should the command return an error, will cause the invoking session to exit.
  The special name "any",
   
    \set exit_on=any
       
  will force the session to exit if any command returns an error. And the
  special name `none`,
   
    \set exit_on=none
       
  Indicates that no command (other than exit itself) should cause the session to
  exit.
   
  The typical use of exit_on would be to cause jsqsh to exit if a SQL statement
  returns an error which would usually be associated with the "go" command:
   
    \set exit_on=go
       
## Understanding errors

  JSqsh commands are very much like shell commands, each command 
  executed "exits" with an error code, with 0 indicating success and a non-zero 
  value indicating an error (the special `${rc}` variable can be used to see the 
  exit value of the most recently executed command).
   
  Most people using this variable will probably be interested in having 
  jsqsh exit only when invoking the `go` command because a failure probably 
  means the statement executed failed.  JSqsh has no specific understanding
  of what a specific JDBC driver may consider a failure--meaning it cannot
  classify one failure from another.  JSqsh simply deterines a failure if
  the JDBC driver throws an exception of any type (note that typically 
  database warnings or informational messages do now throw an exception).
   
## See also

  [[\go|go]]
