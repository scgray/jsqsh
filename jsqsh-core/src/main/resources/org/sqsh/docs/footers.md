## Variable

  `footers` - Controls the display of query footer information
   
## Description

  The `${footers}` variable can be used to turn on or off the display of
  query footer information in query results. Footer information includes
  the row count, update count, and query timing that is normally displayed
  after each result set is displayed. The default value is `true`.

  For example:

    1> select 1 as "One" from sysibm.sysdummy1;
    +-----+
    | One |
    +-----+
    |   1 |
    +-----+
    1 row in results(first row: 0.000s; total: 0.000s)
    1> \set footers=false
    1> select 1 as "One" from sysibm.sysdummy1;
    +-----+
    | One |
    +-----+
    |   1 |
    +-----+
    1>

## See also

  `headers`
