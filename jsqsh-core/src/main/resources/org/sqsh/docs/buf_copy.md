## Synopsis

`\buf-copy src-buf [dst-buf]`
              
## Description

The `\buf-copy` command is utilized to copy the contents of one
SQL buffer into another SQL buffer, replacing the contents of
the receiving buffer.
              
History buffers may be referenced using the special reference
notation `!..`, where each dot indicates how far back in
history (`!.` is the current buffer `!..` the previous, and so on).
Or with the special reference notation `!N` where **N** refers to 
the **N**th historical buffer.
              
For example:
              
    1> \history
    (1) select * from x
    (2) select * from y
    (3) select * from z
    1> select count(*) from
    2> \buf-copy !2
    1> select * from y
    2> 

The current buffer (containing `select count(*) from`) is replaced
with the contents of history buffer #2 (`select * from y`) and the
prompt is left following that statement.
           
## Options

### src-buf

Indicates the buffer that you are copying from.
              
### dst-buf

Indicates the buffer you are copying to. If not provided 
this default to `!.`.

## See also

[[buffers]], [[\history|history]]
