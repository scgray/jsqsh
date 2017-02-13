## Synopsis

`\call [-f file] [-i] [arg ...]`
              
## Description

NOTE: The `\call` command may not be as necessary with the addition of jsqsh's
special handling of the CALL statement.  See [[storedprocs]] for details. 

The `\call` command is similar to the `\go` command except that the SQL contained 
the the SQL buffer is treated as a callable (prepared) statement. The arguments 
for the statement can be supplied either as arguments on the command line, or 
can be read from a file full of comma separated values (CSV format).
   
## Options

### --file=*file* (-f *file*)

Indicates that the arguments for the query should be pulled from a CSV file, 
provided. See **File input parameters**, below.

### --ignore-header (-i)

If a CSV file is to be used for input, this indicates that the first line of 
the input file is assumed to be header information and discarded. 
   
## Command line parameters

The simplest form of executing `\call` is to pass the parameters to the
statement on the command line, such as:
   
    1> select * from sysobjects where name = ?
    2> \call syscolumns
      
Or, if you want to call a stored procedure, you can do:
   
    1> {call sp_who(?)}
    2> \call 112
      
See **Typed parameters**, below for how to deal with parameters of types other 
than strings.
      
## File input parameters

Perhaps the most powerful feature of the `\call` command is its ability
to utilize an input comma separated values (CSV) file for a source
of arguments to the query or stored procedure.  In this case, the
query is executed once for each line in the source file.
   
For example, assuming that you have a file called `test.csv`  that looks 
like:
   
    1,2,3
    4,5,6
      
you can do:
   
    1> select ?, ?, ?
    2> \call --file test.csv
      
which will result in:
   
    +---+---+---+
    |   |   |   |
    +---+---+---+
    | 1 | 2 | 3 |
    +---+---+---+
    1 row in results
    ok. (first row: 2ms; total: 2ms)
    +---+---+---+
    |   |   |   |
    +---+---+---+
    | 4 | 5 | 6 |
    +---+---+---+
    1 row in results
    ok. (first row: 0ms; total: 1ms)
    
In this example, the fields in the input file exactly match those
of the query being executed, however in cases where you wish to
selectively utilize fields in the input file, you can specify 
the field number from the file on the command line, like so:
   
    [T:]#N
      
where `T:` is the optional type (see **Typed parmeters**, below) and
*N* refers to the field number from the input file you wish to utilize,
starting from 1. For example, using our example above:
   
    1> select ?, ?, ?
    2> \call --file test.csv #3 #2 #1
     
    +---+---+---+
    |   |   |   |
    +---+---+---+
    | 3 | 2 | 1 |
    +---+---+---+
    1 row in results
    ok. (first row: 2ms; total: 2ms)
    +---+---+---+
    |   |   |   |
    +---+---+---+
    | 6 | 5 | 4 |
    +---+---+---+
    1 row in results
    ok. (first row: 0ms; total: 1ms)
    
using this syntax, field can be selectively used, rearranged, or discarded 
as desired.
   
## CSV file format
   
The following assumptions are made when interpreting the contents of
the CSV file:
   
* It conforms to: http://www.creativyst.com/Doc/Articles/CSV/CSV01.htm
* Completely empty fields are treated as NULL values.
* Zero-length strings should be given as fields with ""
     
Note that this lines up exactly with how the "csv" display style outputs its 
data (hint hint).
   
## Mixing file and command line parameters

If desired, you may also mix command line parameters and file based
parameters together, like so:
   
    1> select ?, ?, ?
    2> \call --file test.csv #3 a #1
      
    +---+---+---+
    |   |   |   |
    +---+---+---+
    | 3 | a | 1 |
    +---+---+---+
    1 row in results
    ok. (first row: 2ms; total: 2ms)
    +---+---+---+
    |   |   |   |
    +---+---+---+
    | 6 | a | 4 |
    +---+---+---+
    1 row in results
    ok. (first row: 0ms; total: 1ms)
   
## Typed parameters

For most platforms, the parameters passed to a prepared statement must
be of the appropriate datatype for the query. For these cases, the `\call`
command allows a datatype prefix for the arguments provided. These
prefixes are:
   
Argument  | Description
----------|------------------------------------
S:*value*   | Specifies a string value
C:*value*   | Specifies a character value
Z:*value*   | Specifies a boolean value
D:*value*   | Specifies a double value
I:*value*   | Specifies an integer value
J:*value*   | Specifies a long (64 bit integer) value
R:        | (Oracle or DB2) Output a REFCURSOR 

The `R:` argument acts as a place holder for dealing with Oracle or DB2
REFCURSOR output parameters. After the call is executed, the contents 
of the refcursor will be displayed.
      
for example:
   
    1> select * from sysobjects where id = ?
    2> \call I:1234
      
or:
   
    1> select ?, ?, ?
    2> \call --file test.csv #1 I:#2 #3
      
indicates that field #2 in test.csv is an integer field.

## REFCURSORS

Unlike most "normal" databases, Oracle and DB2 stored procedures are incapable
of directly streaming result sets back to the user and procedures are
instead expected to return results via special output parameters of type
REFCURSOR.  For example, the following procedures:
   
    create or replace procedure ref_example (
        i_owner in varchar2
        o_rc1   out sys_refcursor
    )
    as
    begin
        open o_rc1 for
        select * 
          from SYS.ALL_OBJECTS
         where OWNER = i_owner
    end;
 
If you were to just execute the procedure like so:

    1> {call ref_example (?)}
    2> \call SYS
      
you would get no results back. Instead, you must let `\call` know that
the second argument is a refcursor with the special `r:` place holder:
   
    1> {call ref_example (?, ?)}
    2> \call SYS r:
      
upon execution, jsqsh will open the REFCURSOR argument and display its
results just as if it were a normal result set.
      
## See also

[[\go|go]], [[storedprocs]], [[style]]
