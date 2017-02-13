## Variable

  `expand` - Determines if SQL will have variable expansion performed

## Description

  The `${expand}` variable is a boolean, accepting either `false` (the default)
  or `true`. The value of this variable will be used to determine if sqsh
  will attempt to expand SQL statements being executed of any sqsh variables
  they may contain before execution.
  
  The `expand` variable is `false` by default for safety. JSqsh doesn't know
  the dialect of SQL of that target database and it is possible that `$foo` may
  be a valid expression or identifier and variable expansion could cause 
  unwanted side effects and possible data loss.
  
## See also

  [[\go|go]]
