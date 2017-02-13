## Variable

   `rc` - The return value of the last executed command

## Description

   Each jsqsh command that is executed, such as `go` (`\go`) returns a 
   success (0) or failure (not-zero) indicator. This return value can be
   accessed via the `${rc}` variable.
   
   Each time a command fails, the `${fail_count}` session variable is 
   automatically incremented. When jsqsh exits, the exit code is determined 
   by adding together the value of `${fail_count}` across all sessions
   that were executed within jsqsh. That is, if the total is 0, then jsqsh
   exits with 0, otherwise the exit code indicates the number of failures
   encountered during its execution.
   
## See also

   [[fail_count]]
