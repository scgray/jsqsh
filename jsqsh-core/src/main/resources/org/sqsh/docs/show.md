## Synopsis

See **Sub-commands** for specific command details

## Description

The `\show` command can be used to query about various aspects of the
remote server by polling the JDBC driver for information. This
command is boken into several sub-commands
   
Note that all object names provided to show are interpreted as follows:
   
If the name is not surrounded by double quotes, then the name is normalized
according to the database specific normalization rules (these rules are
defined in the internal jsqsh driver definition, and not all database
vendors are currently properly defined, see `\help drivers` for details).
So, for example, the following run while connected to DB2:
       
    \show tables mytab
   
is the equivalent of doing:
   
     \show tables MYTAB
       
because DB2 name normalization always converts to upper case, and matching
is case sensitive in DB2, so the table must be named "MYTAB" to get results
back.  If you wish to force a specific case, surround the name with double
quotes, like:
   
     \show tables "mytab"
   
which forces the name to be matched in exactly the case specified.
   
Please note that, other than the above notes, the behavior of describe in 
terms of wild card characters and name matching is entirely at the mercy
of how the JDBC driver is implemented.

## Scope of results

Many `\show` commands will attempt to automatically restrict the results to 
the current catalog and schema of the user's session.  To widen the scope, 
wildcards may be used. For example,
   
    \show tables
   
will automatically attempt to restrict the list of tables returned to the
current catalog and schema that the session resides in. The following returns
all tables in the current catalog and schema that start with a "p":
   
    \show tables p%
      
and the following shows all tables that start with a "p" in all schemas:
   
    \show tables %.p%
      
and the following shows all tables that start with a "p" across all catalogs
(databases) and all schemas:
   
    \show tables %.%.p%

## Sub-commands   

### \show attributes

`\show attributes [-p pattern] [[[catalog.]schema-pattern.]type-pattern]`
   
Displays the attributes of a user defined type (UDT).  The `-p` option
may be used to provide additional filtering for attributes matching the
provided pattern. 
       
### \show catalogs

`\show catalogs`
   
Shows the set of catalogs (typically databases) that are available 
within the target server. This command has no options.
       
### \show client info

`\show client info`
       
Displays the client driver information properties (if any).
   
### \show column privs

`\show column privs [-p col-pattern] [[[catalog.]schema-pattern.]obj-pattern]`
       
 Displays permissions for coluns of a table. The "-p" flag may be used to
 further restrict the output to just columns matching the specified pattern.
       
### \show columns

`\show columns [-e] [-p col-pattern] [[[catalog.]schema-pattern.]obj-pattern]`
       
Displays columns for a table or set of tables. The `-e` flag can be used
to reduce the number of columns displayed to what is considered the
"essential" set of columns. The `-p` flag may be used to restrict the
output to just columns matching the specified pattern

### \show driver version
       
`\show driver version`
   
Displays JDBC driver version information.
       
### \show exported keys

`\show exported keys [[catalog.]schema.]obj-name`
   
Displays a description of the foriegn key columns that reference a 
given table's primary key columns (the foreign key is exported by a
table).
   
### \show features

`\show features`
   
Displays a list of supported server features.
    
### \show function params

`\show function params [-e] [-p param-pattern] [[[catalog.]schema-pattern.]func-pattern]`
       
Displays parameter information for a function or set of functions.
The function-pattern argument can be used to filter to specific
functions. The `-e` flag limits the set of columns returned those that
are most likely to prove useful. The -p flag allows for an additional
pattern to filter only specific parameters.

### \show functions
       
`\show functions [-e] [[[catalog.]schema-pattern.]func-pattern]`
       
Displays a list of functions matching the provided search criteria. The
`-e` flag limits the set of columns returned to those that are considered
most likely to be useful.

### \show imported keys
   
`\show imported keys [[catalog.]schema.]table`
   
Retrieves a description of the primary key columns that are referenced 
by the given table's foreign key columns (the primary keys imported 
by a table)
       
### \show primary keys

`\show primary keys [[catalog.]schema.]table`
   
Displays the primary keys for a given table or set of tables.

### \show procedure params
       
`\show procedure params [-e] [-p param-pat] [[[catalog.]schema-pattern.]proc-pattern]`
       
Displays parameter information for a procedure or set of procedure.
The procedure-pattern argument can be used to filter to specific
procedures. The `-e` flag reduces the display output to columns that
are most likely to be considered useful. The `-p` flag can be used to
further filter the list of parameters.
       
### \show procedures

`\show procedures [[[catalog.]schema-pattern.]proc-pattern]`
       
Displays a list of procedures matching the provided search criteria.
       
### \show server version

`\show server version`
   
Shows server version and product information.

### \show schemas
       
`\show schemas [[catalog.]schema-pattern]`
       
Lists the available schemas in a catalog
       
### \show super tables

`\show super tables [[[catalog.]schema-pattern.]table-pattern]`
       
Retrieves a description of the table hierarchies defined in a particular 
schema in this database.
       
### \show super types

`\show super types [[[catalog.]schema-pattern.]type-pattern]`
   
Retrieves a description of the user-defined type (UDT) hierarchies defined 
in a particular schema in this database.
       
### \show table privs

`\show table privs [[[catalog.]schema-pattern.]table-pattern]`
   
Displays information able privileges on a given table.        
      
### \show tables

`\show tables [-e] [-T type] [[[catalog.]schema-pattern.]table-pattern]`
   
Displays information about any tabular object that appears in the
server, this includes tables, views, synonyms, etc.  The `-T` flag can
be used to filter on only specific table types (the set of available
types can be determined using `\show table types`).  The `-e` flag 
reduces the number of columns returned to just basic information 
about the tables (catalog, schema, name, and type).
       
### \show table types

`\show table types`
    
Shows the available types of tables in the database. The names returned
in this list, can be used to filter tables in the `\show tables` command.
       
### \show types

`\show types [-e]`
   
Lists the available data types in the server. The `-e` flag can be used
to produce a more simplified set of information about the available types.
       
### \show user types

`\show user types [[[catalog.]schema-pattern.]type-pattern]`
   
Lists the available data user defined types in the server. The 
type-pattern argument may be used to restrict the list of types
to a specific set.
       
### \show version columns

`\show version columns [[[catalog.]schema.]table`
   
Retrieves a description of a table's columns that are automatically 
updated when any value in a row is updated. They are unordered
       
## Common options

The following options are common to many of the object types that are
displayed via the show commands:
   
### --pattern=*pattern* (-p *pattern*)

A SQL pattern to be used to filter the set of information returned by the command.

### --type=*pattern* (-T *pattern*)

Used by `\show tables` to limit the tables returned to only specific types 
of tables.

### --essential (-e)

This option is available for some commands that can return a
large number of columns, and attempts to reduce the number of
columns down to an "essential" set. That is, a set of columns
that are thought to be the most useful.
   
## See also

[[\describe|describe]], [[\tables|tables]], [[\procs|procs]]
