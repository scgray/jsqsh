## Synopsis

`\tree [--table-pattern=pat] [--schema-pattern=pat]`

## Description

A tree view of all tables.

## Options

### --table-pattern=*pattern* (-p *pattern*)

A SQL pattern to be used to filter the set of object names that are displayed.

### --schema-pattern=*pattern* (-s *pattern*)

A SQL pattern to be used to filter the set of object schema names that are 
displayed. For most databases, the "schema name" refers to the name of the 
owner of the object.

### type

This can be used to restrict the types of objects that are displayed. If 
type is not provided, then all object types are returned. 
Available types are:

Type    | Description
--------|--------------------------
user    | Displays only user-created tables
system  | Displays only system tables
view    | Displays only views
alias   | Displays only aliases
synonym | Displays only synonyms
