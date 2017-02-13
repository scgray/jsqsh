## Synopsis

`\history [-a]`
              
## Description

Displays a history of SQL statements executed. For example:

    1> \history
    (1)  select count(*) as 'Count' from $t
    (2)  select 1
    (3)  select top 3 * from sysdatases
    (4)  select top 3 * fromsysdatabases
    (5)  select top 3 * from sysdatabases
    (6)  use mydb
    (7)  select * from STATISTIC_SAMPLE_TYPE
         where SAMPLE_DATE >= '1/1/2012'
    (8)  exec sp_myproc @x = 10
    (9)  sp_who
    (10) select * from syscolumns

The command history displayed in chronological order with the
final entry shown being the most recent statement executed.

History entries are added whenever either the [[\go|go]] or [[\reset|reset]]
commands are executed. See [[buffers]] for an explanation of how
to work with SQL buffers.
           
## Options

### --all (-a)

  Show all of every SQL statement executed. If this flag is not
  provided only the first 10 lines of any SQL statement will be
  displayed.

## See also

[[\buf-edit|\buf_edit|buf-edit|\buf_edit]], [[\buf-copy|\buf_copy|buf-copy|\buf_copy]], [[\buf-append|\buf_append|buf-append|\buf_append]], 
[[\buf-load|\buf_load|buf-load|\buf_load]], [[buffers]], [[\go|go]], [[\reset|reset]]
