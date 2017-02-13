## Synopsis

`\go [-i table] [-vname=value] [-h] [-f] [-m style] [-n repeat] [-t sec]`
              
## Description

Executes the SQL statement contained in the current buffer.  This command
is also provided by a built-in alias of `go`. For example, you can use:

    1> select * from foo
    2> \go
    +-----+
    | col |
    +-----+
    |   1 |
    |   2 |
    +-----+
    (2 rows in results(first row: 0.000s; total: 0.002s)

or

    1> select * from foo
    2> go
    +-----+
    | col |
    +-----+
    |   1 |
    |   2 |
    +-----+
    (2 rows in results(first row: 0.000s; total: 0.003s)

## Options

### --crosstab=*vcol,hcol,dcol* (-c *vcol,hcol,dcol*)

Produces a crosstab of the actual result set. Where:

  * *vcol* indicates the column in the original result set that will
    drive the rows (the vertical column headers) of the final result
    table.  This can either specify a column name or a column number
    (starting from 1) in the original result set.
  * *hcol* indicates the column in the original result set that will
    drive the horizontal column headers of the final result table.
    This can either specify a column name or a column number (starting
    from 1) in the original result set.
  * *dcol* indicates the column in the original result set that will
    provide the data for the table.

for example, given the following result set:

    1> select * from salesbystate
    2> go
    +-------+-----------+----------+
    | STATE | DAYOFWEEK |    SALES |
    +-------+-----------+----------+
    | NJ    | Mon       | 14.20000 |
    | NJ    | Tue       | 11.40000 |
    | NJ    | Wed       | 19.30000 |
    | CA    | Mon       |  4.10000 |
    | CA    | Tue       |  8.30000 |
    | CA    | Wed       | 44.20000 |
    | NJ    | Thu       | 17.10000 |
    | AR    | Tue       |  4.30000 |
    +-------+-----------+----------+
    (8 rows in results(first row: 0.000s; total: 0.002s)

you can produce a sales by dayofweek for each state like so:

    1> select * from salesbystate
    2> go -c dayofweek,state,sales
    +-----------+---------+----------+----------+
    | DAYOFWEEK |      AR |       CA |       NJ |
    +-----------+---------+----------+----------+
    | Mon       |  [NULL] |  4.10000 | 14.20000 |
    | Tue       | 4.30000 |  8.30000 | 11.40000 |
    | Wed       |  [NULL] | 44.20000 | 19.30000 |
    | Thu       |  [NULL] |   [NULL] | 17.10000 |
    +-----------+---------+----------+----------+
    (8 rows in results(first row: 0.001s; total: 0.002s)

similarly this could have been done by column position:

    1> select * from salesbystate
    2> go -c 2,1,3

note that the footer (containing the row counts and timings)
will always reflect that of the underlying result set that
produces the crosstab.

### --display-style=*style* (-m *style*)

Specifies a custom display style for showing the results of the
execution. The available styles may be found by running `\help style`.
For example:

    1> select * from salesbystate
    2> go -m csv
    NJ,Tue,13.2
    NJ,Wed,8.4
    CA,Mon,11.1
 
or
       
    1> select * from salesbystate
    2> go -m simple
    STATE | DAYOFWEEK | SALES
    ------+-----------+------
    NJ    | Tue       |  13.2
    NJ    | Wed       |   8.4
    CA    | Mon       |  11.1

Note that the set of valid display styles depends upon the type of
session that is currently established. For example, attempting to set
a SQL display style, such as `pretty` for a non-SQL session will cause
an error.

### --insert=*table* (-i *table*)

Causes the results to be generated as INSERT statements for 
the specified table name. This flag causes the value provided by 
`-m` to be ignored.

    1> select * from salesbystate
    2> go --insert=FOO
    INSERT INTO TABLE FOO (STATE, DAYOFWEEK, SALES) VALUES
     ('NJ', 'Mon', 14.20000)
    INSERT INTO TABLE FOO (STATE, DAYOFWEEK, SALES) VALUES
     ('NJ', 'Tue', 11.40000)
    ...
  
### --no-footers (-F)

Toggles the display of result footer information (row count and timing
information). This flag effectively flips the  value of ${[[footer]]} for
the duration of the query execution.

For example, given the result below:

    1> select * from salesbystate fetch first 2 rows only
    2> go
    +-------+-----------+----------+
    | STATE | DAYOFWEEK |    SALES |
    +-------+-----------+----------+
    | NJ    | Mon       | 14.20000 |
    | NJ    | Tue       | 11.40000 |
    +-------+-----------+----------+
    2 rows in results(first row: 0.004s; total: 0.005s)

turning off footers would produce:

    1> select * from salesbystate fetch first 2 rows only
    2> go -F
    +-------+-----------+----------+
    | STATE | DAYOFWEEK |    SALES |
    +-------+-----------+----------+
    | NJ    | Mon       | 14.20000 |
    | NJ    | Tue       | 11.40000 |
    +-------+-----------+----------+

### --no-headers (-H)

Toggles the display of column header information. This flag effectively 
flips the value of ${[[header]]} for the duration of the query execution.

For example, given the result below:

    1> select * from salesbystate fetch first 2 rows only
    2> go
    +-------+-----------+----------+
    | STATE | DAYOFWEEK |    SALES |
    +-------+-----------+----------+
    | NJ    | Mon       | 14.20000 |
    | NJ    | Tue       | 11.40000 |
    +-------+-----------+----------+
    2 rows in results(first row: 0.004s; total: 0.005s)

turning off headers would produce:

    1> select * from salesbystate fetch first 2 rows only
    2> go -H
    +-------+-----------+----------+
    | NJ    | Mon       | 14.20000 |
    | NJ    | Tue       | 11.40000 |
    +-------+-----------+----------+
    2 rows in results(first row: 0.004s; total: 0.005s)

### --repeat=n (-n)

Execute the SQL n times, reporting the total and average execution times. 
When this flag is provided the variable ${iteration} will be available 
during variable expansion and will reflect which iteration the query 
is being executed in (starting from 0).

For example, on if ${[[expand]]} is set to true (which allows for variables
contained in SQL statements to have jsqsh variables expanded), the the 
following would produce:

    1> values ('Iteration', ${iteration})
    2> go -n 2
    +-----------+---+
    | 1         | 2 |
    +-----------+---+
    | Iteration | 0 |
    +-----------+---+
    1 row in results(first row: 0.004s; total: 0.004s)
    +-----------+---+
    | 1         | 2 |
    +-----------+---+
    | Iteration | 1 |
    +-----------+---+
    1 row in results(first row: 0.003s; total: 0.003s)
    2 iterations (total 0.007s, 0.003s avg)

### --timeout=*sec* (-t *sec*)

Places a time restriction (in terms of seconds) on the query.  If the 
underlying driver supports setting query timeouts, then the driver will 
be asked to do the timeout processing. If the driver does not support such 
a feature then jsqsh will automatically cancel the query when the timeout 
period has been reached.

### --var=*name=value* (-v *name=value*)

Allows the setting of a jsqsh configuration variable for the duration of the
command. For example, to change the delimiter for the `csv` display style,
you can do the following:

    1> values('a','b')
    2> \go --no-headers -m csv --var csv_delimiter='|'
    a|b

This option may be specified multiple times to provide the value for multiple
variables, such as:

    1> values(',','b')
    2> \go --no-headers -m csv --var csv_delimiter='|' --var csv_quote="'"
    ','|b

## Statement terminator

JSqsh provides a short-hand mechanism for executing SQL statements by terminating
your statement using the jsqsh statement terminator character (which is `;` by
default).  For example:

    1> select * from foo;
    +-----+
    | col |
    +-----+
    |   1 |
    |   2 |
    +-----+

This works as follows:

Every time you hit enter, jsqsh checks the line that you just typed to see if it ends 
with the statement terminator character, if it is then it assumes you want to 
execute the current statement:

    1> select 'hello world' from sysibm.dual;
    +-------------+
    | 1           |
    +-------------+
    | hello world |
    +-------------+
    1 row in results(first row: 0.4s; total: 0.4s)

JSqsh also takes care to avoid trying to execute the statement you are typing if the 
terminator character occurs within a quoted string or within a comment:

    1> -- This line will not execute;
    2> select 'This line will not execute;
    3>   either' from sysibm.dual;
    +-----------------------------+
    | 1                           |
    +-----------------------------+
    | This line will not execute; |
    |   either                    |
    +-----------------------------+
    1 row in results(first row: 0.0s; total: 0.0s)

In addition, for the case of a semicolon terminator, jsqsh even contains some logic to 
attempt to determine of a semicolon contained at the end of a line is part of the SQL 
itself, or is to be used to execute the current statement.  For example, in SQL PL 
blocks, a semicolon can be used to indicate the end of a statement, but not the end 
of the SQL itself. In this case, jsqsh will attempt to be "smart" and figure out where 
the real end of the statement is:

    1> CREATE PROCEDURE P1 (IN V1 INT, OUT V2 INT)
    2>   LANGUAGE SQL 
    3>   BEGIN 
    4>      -- Note the trailing semicolon. This will not cause jsqsh to execute
    5>      SET V2 = V1 * 2;
    6>      -- But the one after the END will
    7>   END;
    0 rows affected (total: 0.3s)

**IMPORTANT NOTE**: This SQL parsing logic is crude, and it can often get things 
wrong, which can be seen if jsqsh attempts to execute a block before you are 
done typing it in, or it ignores the semicolon that you intended to actually run 
the procedure.  There are two ways you can deal with this problem.

To avoid the ambiguity of the semicolon as a command terminator, jsqsh allows the 
terminator character to be changed via the `\set` command:

    [localhost][gray] 1> \set terminator=@
    [localhost][gray] 1> select 'hello world' from sysibm.dual@
    +-------------+
    | 1           |
    +-------------+
    | hello world |
    +-------------+
    1 row in results(first row: 0.0s; total: 0.0s)

## See also

[[style]], [[header]], [[footer]], [[timer]]
