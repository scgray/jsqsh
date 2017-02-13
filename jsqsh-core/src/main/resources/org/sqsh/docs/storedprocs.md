# Calling stored procedures

JSqsh provides special support for calling stored procedures that require
output (OUT) or input/output (INOUT) parameters. 
   
## JDBC escape syntax
   
The JDBC standard defines a canonical way of executing stored procedures 
in a manner that abstracts away the specifics of how procedures are invoked
for a given platform.  The following example executes a procedure passing one
input argument and retrieving one output argument using this syntax:
   
    1> { call my_test_proc(10, ?) };
    +-------------+
    | Param #1    |
    +-------------+
    | Fred        |
    +-------------+
      
here, a question mark (`?`) is used to denote an output parameter. After
execution, the contents of that output parameter is displayed as shown.
   
In addition, many platforms allow you to also get a return value from the
stored procedure.  To do this in JDBC escape syntax do:
   
    1> { ?= call my_test_proc(10, ?) };
    +-------------+-------------+
    | Return Code | Param #2    |
    +-------------+-------------+
    |           0 | Fred        |
    +-------------+-------------+
      
In the case of input/output parameters, jsqsh slightly extends the JDBC
standard with `?=<value>` which indicates that the parameter is an INOUT
parameter, and that the initial input value should be `<value>`, like so:
   
    1> { ?= call double_my_inout_param_value(10, ?=321) };
    +-------------+-------------+
    | Return Code | Param #2    |
    +-------------+-------------+
    |           0 | 642         |
    +-------------+-------------+

## Automatic "CALL" handling

The JDBC escape syntax above is awkward to work with, so jsqsh will attempt
to automatically recognize when you are trying to call a procedure.  It does
so according to the following rules:
   
  1. The first keyword in your statement is `CALL`
  2. There are one or more parameter markers (`?`) following the CALL keyword
     (`?` characters contained in comments or string constants are ignored)
  
If both of these are true, then jsqsh automatically takes your statement
and attempts to make it a JDBC escape syntax statement.  For example, 
executing the following:
   
    1> call my_proc(10, ?);
   
will cause jsqsh to turn it into:
   
    { ?= call my_proc(10, ?) }
   
and execute the statement.   

## Output cursors

Unfortunately, there is no standard in JDBC for the handling of CURSOR 
output parameter types, so each JDBC driver deals with them differently.
As a result, jsqsh currently only has an understanding of how to handle
CURSOR output parameters for IBM DB2 and Informix, and Oracle.  
   
When faced with an OUTPUT parameter of type cursor, jsqsh will display
the results like so:
   
    1> call my_cursor_output(?);
      
    Parameter #2 CURSOR:
    +---+
    | 1 |
    +---+
    | 5 |
    +---+

    +-------------+
    | Return Code |
    +-------------+
    |           0 |
    +-------------+
