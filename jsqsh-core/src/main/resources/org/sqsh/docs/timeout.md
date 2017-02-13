## Variable

  `timeout` - Controls the number of seconds before a query is timed out

## Description

  The `${timeout}` variable is used to control the number of seconds before a
  query is timed out.  When the variable is set from the sqshrc file or from
  a session that has not yet established a connection, it defines the default
  timeout for all connections. When set from the context of an already
  established connection, it controls only the timeout for queries on that
  connection.
   
  If a JDBC connection is established, and the driver supports query timeouts,
  then the JDBC driver is asked to time the queries out (the behavior of which
  is dependant upon the driver vendor).  If the connection does not support
  query timeouts, then jsqsh will automatically cancel the current query when
  the timeout interval has been exceeded.
   
## See also

  [[\go|go]]
