## Synopsis

`\commit`

## Description

The `\commit` command commits the current transaction by calling the JDBC `commit()`
API call.  Most databases have a statement that can (and probably should) be used
(such as `COMMIT TRANSACTION`), this command can be used in cases in which the such
a command may not exist.

## See also

[[\rollback|rollback]]