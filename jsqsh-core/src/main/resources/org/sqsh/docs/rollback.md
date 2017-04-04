## Synopsis

`\rollback`

## Description

The `\rollback` command rolls back the current transaction by calling the JDBC `rollback()`
API call.  Most databases have a statement that can (and probably should) be used
(such as `ROLLBACK`), this command can be used in cases in which the such
a command may not exist.

## See also

[[\commit|commit]]