## Variable

  `headers` - Controls the display of column headers
   
## Description

  The `${headers}` variable can be used to turn on or off the display of
  column header information in query results.  The default value is `true`.

  For example:

    1> select 1 as "One" from sysibm.sysdummy1;
    +-----+
    | One |
    +-----+
    |   1 |
    +-----+
    1 row in results(first row: 0.000s; total: 0.000s)
    1> \set headers=false
    1> select 1 as "One" from sysibm.sysdummy1;
    +-----+
    |   1 |
    +-----+
    1 row in results(first row: 0.000s; total: 0.000s)

## See also

  [[footers]]
