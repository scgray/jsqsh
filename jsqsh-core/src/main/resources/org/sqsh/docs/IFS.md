## Variable

`IFS` - Internal field separater for processing backtick command output

## Description

Determines how the output of a command executed via backticks (\`) is to
broken up into words. By default, it is broken up by whitespace characters
\\n, \\r, \\t, and space. 

      1> \echo `echo a b c`
      a b c
      1> \echo `echo a:b:c`
      a:b:c

${IFS} can be used to change this behavior, such as:

      1> \set IFS=:
      1> \echo `echo a:b:c`
      a b c

When setting the field separator the following special characters are 
recognized:

* `\n` - Newline
* `\r` - Carriage return
* `\t` - Tab
* `\s` - Equivalent of " \\n\\r\\t"

If ${IFS} contains more than one character, **ANY** of the provided characters
will be considered a separator. 
