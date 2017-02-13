## Synopsis

`\echo [-n] [string] [...]`
              
## Description

Echos the string(s) to standard out.
           
## Options

### --no-newline (-n)

Disables the output of a newline character at the end of the output. For
example, given a script containing:

    \echo -n hel
    \echo lo

the final output would be:

    hello

## Examples

Display a static string:

    1> \echo "Hello world!"
    Hello world!

Display text with a variable
 
    1> \set x=10
    1> \echo "Value of x is $x"
    Value of x is 10

## See also

[[\set|set]]
