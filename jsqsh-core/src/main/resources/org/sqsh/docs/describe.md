## Synopsis

`\describe [-a] [[catalog.]schema-pattern.]object-pattern`
              
## Description

Displays the database structure for a provided object name. This functionality relies 
upon support from the underlying JDBC driver and thus may fail against some 
platforms. 
   
The names provided (catalog, schema-pattern, and object-pattern) are 
all interpreted as follows
   
If the name is not surrounded by double quotes, then the name is normalized
according to the database specific normalization rules (these rules are
defined in the internal jsqsh driver definition, and not all database
vendors are currently properly defined, see "\help drivers" for details).
So, for example, the following run while connected to DB2:
       
    \describe mytab
   
is the equivalent of doing:
   
    \describe MYTAB
       
because DB2 name normalization always converts to upper case, and matching
is case sensitive in DB2, so the table must be named "MYTAB" to get results
back.  If you wish to force a specific case, surround the name with double
quotes, like:
   
    \describe "mytab"
   
which forces the name to be matched in exactly the case specified.
   
Please note that, other than the above notes, the behavior of describe in 
terms of wild card characters and name matching is entirely at the mercy
of how the JDBC driver is implemented.
   
## Options

### --all (-a)

Shows all available information about the table that the JDBC driver provides. 
Without this flag `\describe` will attempt to keep the output to the more useful data.

### catalog

The catalog (or database for some vendors) in which the object resides. If not 
provided then the current catalog is assumed.

### schema-pattern

The name of the schema in which the object resides, which may include SQL wildcard 
characters. If the catalog name is provided and the schema is not, like:
                  
    \describe mydb..mytable
               
then all schemas within the catalog are matched.  If the catalog name is not provided 
and the schema is not provided, like:
               
    \describe mytable

Then:

1. If the JDBC driver is 1.7 or later and supports the
   necessary API's, to retrieve the current schema, then the 
   current schema is assumed.  Otherwise...
2. If the jsqsh driver definition has a query installed to
   fetch the current schema from the database, then the 
   current schema is assumed. Otherwise...
3. All schemas are matched
               
### object-pattern

The object name to be described, which may contain SQL wild cards to match multiple 
objects.
   
## See also

[[\show|show]], [[\tables|tables]], [[\drivers|drivers]]
