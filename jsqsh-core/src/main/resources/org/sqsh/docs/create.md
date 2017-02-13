## Synopsis

`\create [-p] table_name`
              
## Description

The `\create` command generates a CREATE TABLE statement based upon the name of the 
table provided on the command line.
   
The definition of the table is determined by asking the JDBC driver for metadata 
information about the table, so the CREATE statement will only be as accurate as 
the driver reports. In addition, the current implementation of the `\create` command 
does not include anything other than column names, types, and nullability.  That 
is, it does not include any primary or foreign key information or check constraints.
   
## Options

### --print (-p)

Causes the CREATE TABLE statement to just be printed to the screen rather than 
placing it into the SQL buffer for execution.
                
## See also

[[\select|select]]
