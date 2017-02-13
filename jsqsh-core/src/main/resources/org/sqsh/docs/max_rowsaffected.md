## Variable

  `max_rowsaffected` - Stop processing results after "rows affected" is displayed

## Description

  This option is provided primarily to support buggy drivers.  Certain JDBC
  drivers (e.g. the Apache Hive2 driver) don't always property indicate when
  they are done sending results to the client, in which case you'll see 
  situations like this:
   
    1> use default;
    0 rows affected 
    0 rows affected 
    0 rows affected 
    ...
   
  This option forces jsqsh to stop processing after `${max_rowaffected}` rows
  affected have been displayed. Setting this to a value <= 0 (0 is the default)
  indicates that there is no limit on the number of rows affected that can 
  be displayed.
   
## See also
  [[\go|go]]
