## Synopsis

`\eval filename`
              
## Description

The `\eval` command reads in and processes a file full of SQL in the context
of the current session. That is, it treats the SQL and/or jsqsh commands 
contained in the file as if you had typed them at the prompt. The one 
exception to this rule is that the SQL contained in the file will not be
added to your SQL history.
   
## Aliases

This command is automatically aliased to `:e`, so that you may do:
   
    :e filename
      
at the prompt.
   
## Options

### --repeat=*n* (-n *n*)

Execute the SQL contained in the input file *n* times. When this flag is 
provided the variable ${iteration} will be available during variable 
expansion and will reflect which iteration is being executed (starting from 0). 

### filename

The name of the file to execute.
   
## See also

[[\buf-load|\buf_load|buf-load|\buf_load]]
