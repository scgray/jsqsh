## Synopsis

`\unset var_name [var_name ...]`
              
## Description

Unsets one or more jsqsh variables. Note that any variable that appears in
`\help vars` is a special configuration variable and may not be unset with
this command.
              
Removing a session-local variable (created with `\set -x`), will re-expose 
any global variable of the same name. Two calls to [[\set|set]] would need to be 
invoked to completely remove the variable in this case.
  
## Options
  None.

## See also

[[\set|set]]
