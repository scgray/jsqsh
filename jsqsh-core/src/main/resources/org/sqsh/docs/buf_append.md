## Synopsis

`\buf-append src-buf [dst-buf]`
                  
## Description

The `\buf-append` command is utilized to append the contents of one
SQL buffer into another SQL buffer.
                  
History buffers may be referenced using the special reference
notation starting with `!`, for example `!!` indicates the previous
buffer. This syntax is covered under the help topic [[buffers]].
                  
For example:
                  
    \buf-append !5 !.
                    
appends the contents of history buffer #5 into the current SQL
buffer. This could also have been expressed as:

    \buf-append !5

## Options

### src-buf

Indicates the buffer that you are copying from.
                  
### dst-buf

Indicates the buffer you are append to. If not provided this default 
to `!.`.

## See also

[[buffers]]
