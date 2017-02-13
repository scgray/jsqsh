## Synopsis

`\select [-p] [-n] table_name [table_name ...]`
              
## Description

The `\select` command generates a SELECT statement based upon the
set of tables provided as an argument.
   
If more than one table is provided to `\select`, then an appropriate
WHERE clause will attempt to be generated to join the tables together.
If the `-n` flag is provided, then the WHERE clause will be the "natural
join" of the tables--that is, all columns of the same name and type 
will be joined together.  If the `-n` flag is not provided, then the join
will be created by analyzing the foreign key/primary key relationships
between the tables. 
   
The `\select` command requires proper meta-data support from the underlying
JDBC driver and may not work properly for all driver implementations.
   
## Options

### --natural-join (-n)

Cause the join clause that is produced to be based upon the "natural join" 
of the columns in the tables. The natural join attempts to join all columns 
of the same name and datatype.
       
### --print (-p)

Causes the SELECT statement to just be printed to the screen rather than 
placing it into the SQL buffer for execution.
   
### See also

[[\go|go]]
