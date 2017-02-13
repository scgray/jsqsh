## Synopsis

`\prepare`

## Description

The `\prepare` command causes the current SQL buffer to be prepared 
against the server and metadata about the prepared query to be
displayed, such as the columns that will be returned by the query
as well as information about any parameter markers that are contained
in the query.
   
Note that `\prepare` does not actually execute the SQL statement, for that
see the `\call` command

## Options

None

## See also

[[\call|call]]
