## Variable

  `ex_detail` - Controls the level of detail when displaying exceptions

## Description
  The `${ex_detail}` variable can be used to control the amount of information
  produced by exceptions.  The available levels are:
   
  * `low` - Displays just the exception class name and, for SQL exceptions, 
    the SQL code and state.
  * `medium` - (default) Displays the SQL code and state (for SQL exceptions) 
    the text of the exception, and the same information for any nested 
    exceptions.
  * `high` - Displays everything from medium, plus the stack trace for each.
   
## See also

  [[show_exclass]]
