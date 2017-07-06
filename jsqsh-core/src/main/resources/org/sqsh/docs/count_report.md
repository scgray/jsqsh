## Variable

`count_report` - ontrols how often the "count" display style reports statistics

## Description

By default the `count` display style reports metrics on the rows received every 
10,000 rows, this variable allows the update interval to be changed to the value
specified. A value of 0 will cause it to report only upon receiving the last row
of a result set.

## See also

[[style]]
