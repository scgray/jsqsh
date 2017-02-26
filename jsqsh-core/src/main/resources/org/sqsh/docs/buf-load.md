## Synopsis

`\buf-load [-a] filename [dest-buf]`
              
## Description

The `\buf-load` command reads the contents of an external file into
a SQL buffer. By default jsqsh will come with an alias for `\buf-load`
of `:r`, so that you can do:
   
    1> :r filename.sql
      
to read in the filename. If you do not want this behavior, then you
can `\unalias :r` in your `$HOME/.jsqsh/sqshrc` file.
           
## Options

### --append (-a)

Causes the contents of the file to be appended to the SQL buffer, rather 
than replacing its contents.

### filename

The name of the file to be read

### dest-buf

The name of the destination buffer to read the file into. The default 
is `!.` (the current buffer).
             
## See also

[[\history|history]], [[buffers]]
