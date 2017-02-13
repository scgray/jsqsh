## Synopsis

`\reset`
              
## Description

Adds the current SQL command buffer to the list of historical
buffers (viewable with \history) and creates a fresh buffer. This
command is typicallyl used when you are working on a query and decide
to discard it or to save it away so you can resume editing it later.
           
## Options

None.

## Examples

    1> select count(*) 
    2> from t1, t2, t3
    3> \reset
    1>
