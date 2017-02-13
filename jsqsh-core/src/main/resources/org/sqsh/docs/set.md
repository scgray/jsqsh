## Synopsis

`\set [[-x] [-l] name=value]`
              
## Description

Sets a variable or displays all variables. Just like a shell, variables may either
be used to configure jsqsh, or can be used to hold values that are later expanded
on the command line.  For example, the following creates a new variable called
"x" and anywhere `$x` appears on the command line, it will be replaced with the
value of the variable.

    1> \set x=10
    1> \echo "x is $x"
    x is 10

such variables may also be referenced within SQL statements that are executed
provided that the ${[[expand]]} variable is set to `true`. For example:

    1> \set x=10
    1> \set expand=true
    1> select * from foo where c1 = $x;
    +----+----+
    | c1 | c2 |
    +----+----+
    | 10 |  a |
    +----+----+

## Options

If no arguments are provided to `\set`, then the value of all variables is displayed.
              
### --export (-x)

Will cause the variable to be exported to the environment of any processes that are 
spawned by jsqsh during its execution.

### --local (-l)

Sets the value of the variable local to the current session only. No other sessions will 
see it and it will mask similarly named variables in other session.

## See also

[[\echo|echo]], [[\expand|expand]]
