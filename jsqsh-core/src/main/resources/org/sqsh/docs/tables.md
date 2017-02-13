## Synopsis

`\tables [--type type] [[[catalog.]schema-pattern.]object-pattern]`
              
## Description

The `\tables` command is deprecated, it is suggested to use the `\show`
command instead.

Displays information about table objects (as the server defines a table).
This functionality relies upon support from the underlying JDBC driver
and thus may fail against some platforms. 
   
The names provided (catalog, schema-pattern, and object-pattern) are 
all interpreted as follows
   
If the name is not surrounded by double quotes, then the name is normalized
according to the database specific normalization rules (these rules are
defined in the internal jsqsh driver definition, and not all database
vendors are currently properly defined, see `\help drivers` for details).
So, for example, the following run while connected to DB2:
       
    \tables my%
   
is the equivalent of doing:
   
    \tables MY%
       
because DB2 name normalization always converts to upper case, and matching
is case sensitive in DB2, so the table must be named something like "MYTAB" 
to get results back.  If you wish to force a specific case, surround 
the name with double quotes, like:
   
    \tables "my%"
   
which forces the name to be matched in exactly the case specified.
   
Please note that, other than the above notes, the behavior of `\tables` in 
terms of wild card characters and name matching is entirely at the mercy
of how the JDBC driver is implemented.
   
## Options
  
### --all (-a)

Display all metadata description columns instead of the default reduced set.

### --type=*type* (-T *type*)

This can be used to restrict the types of objects that are displayed. If 
type is not provided, then all object types are returned. Available types 
are:
        
Type     | Description
---------|------------------------------------
user     | Displays only user-created tables 
system   | Displays only system tables 
view     | Displays only views
alias    | Displays only aliases
synonym  | Displays only synonyms

### catalog

The catalog (or database for some vendors) in which the object resides. 
If not provided then the current catalog is assumed.

### schema-pattern

The name of the schema in which the object resides, which may
include SQL wildcard characters. If the catalog name is provided 
and the schema is not, like:
                  
    \tables mydb..my%
               
then all schemas within the catalog are matched.  If the catalog
name is not provided and the schema is not provided, like:
               
    \tables my%
               
Then:

1. If the JDBC driver is 1.7 or later and supports the
   necessary API's, to retrieve the current schema, then the 
   current schema is assumed.  Otherwise...
2. If the jsqsh driver definition has a query installed to
   fetch the current schema from the database, then the 
   current schema is assumed. Otherwise...
3. All schemas are matched

### object-pattern

The object name to be described, which may contain SQL wild cards to 
match multiple objects.                

## See also

[[\show|show]], [[\databases|databases]], [[\describe|describe]], [[\procs|procs]], [[\drivers|drivers]]
