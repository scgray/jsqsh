## Synopsis

`\insert [options] table_name`
              
## Description

The `\insert` command executes the current SQL buffer and turns any
result sets that are returned by the query into SQL INSERT statements.
This is similar to executing the `\go` command with  its `-i` option, 
except that a more fine grained control of how the inserts are processed
is available.
   
By default, `\insert` simply displays the INSERT statement(s) to the 
screen, however if the `-s` flag is provided a connection maintained
by a different session may be used to execute the INSERT statements.
   
## Options

### --batch-size=*rows* (-b *rows*)

Specifies the number of rows that should be "batched" together. If the 
`-s` option is not used, then this indicates how often a `go` is displayed 
to the screen. If the `-s` option is used, then the specified number of
INSERT statements is executed at once, followed by a commit.
                  
### --target-session=*id* (-s *id*)

Provides a target session id for execution of the INSERT statements that 
are generated.  The provided session must be connected to a database server to
function.

### --multi-row (-m)

Enables support for platforms that allow multiple rows to be inserted in a 
single INSERT statement. When enabled the batch size (-b) indicates the 
number of rows per insert statement.

### --terminator=*term* (-t *term*)

Changes the batch terminator from the default `go` to the string provided.

## See also

[[\go|go]]
