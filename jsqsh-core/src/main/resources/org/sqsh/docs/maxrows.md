## Variable

  `maxrows` - Limits the size of query results.

## Description

  The `${maxrows}` variable can be used to limit the number of rows returned
  by any individual query in a batch of SQL (the default is 500). A value
  of zero or less indicates that query result sizes should be unlimited.
   
  For certain display styles (see [[style]]), query results are retained
  entirely in memory so it is important the results not be unbounded.
   
## See also

  [[style]]
