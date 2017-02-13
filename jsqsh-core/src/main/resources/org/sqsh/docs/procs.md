## Synopsis

`\procs [[[catalog.]schema-pattern.]proc-pattern]`
              
## Description

Displays information about stored procedure objects in the database server
This functionality relies upon support from the underlying JDBC driver
and thus may fail against some platforms. 
   
The names provided (catalog, schema-pattern, and proc-pattern) are 
all interpreted as follows
   
If the name is not surrounded by double quotes, then the name is normalized
according to the database specific normalization rules (these rules are
defined in the internal jsqsh driver definition, and not all database
vendors are currently properly defined, see `\help drivers` for details).
So, for example, the following run while connected to DB2:
       
    \procs my%
   
is the equivalent of doing:
   
    \procs MY%
       
because DB2 name normalization always converts to upper case, and matching
is case sensitive in DB2, so the table must be named something like "MYPROC" 
to get results back.  If you wish to force a specific case, surround 
the name with double quotes, like:
   
    \procs "my%"
   
which forces the name to be matched in exactly the case specified.
   
Please note that, other than the above notes, the behavior of `\procs` in 
terms of wild card characters and name matching is entirely at the mercy
of how the JDBC driver is implemented.
   
## Options

### catalog

The catalog (or database for some vendors) in which the object resides. 
If not provided then the current catalog is assumed.

### schema-pattern

The name of the schema in which the object resides, which may include 
SQL wildcard characters. If the catalog name is provided and the schema 
is not, like:
                  
    \procs mydb..my%
               
then all schemas within the catalog are matched.  If the catalog
name is not provided and the schema is not provided, like:
               
    \procs my%

Then:

1. If the JDBC driver is 1.7 or later and supports the
   necessary API's, to retrieve the current schema, then the 
   current schema is assumed.  Otherwise...
2. If the jsqsh driver definition has a query installed to
   fetch the current schema from the database, then the 
   current schema is assumed. Otherwise...
3. All schemas are matched
               
### proc-pattern

The procedure name to be described, which may contain SQL wild cards 
to match multiple procedures.        

### See also

[[\show|show]], [[\databases|databases]], [[\describe|describe]], [[\tables|tables]]
