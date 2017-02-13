## Variable

   `fetchsize` - The number of rows per fetch to request from the server

## Description

   The `${fetchsize}` controls the JDBC "fetch size" setting, which requests that
   the driver return the specified number of rows per network request to the
   server.  This variable may not be honor by all drivers, but for those that
   do, it can increase performance at the expense of memory.
