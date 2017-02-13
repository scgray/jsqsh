## Synopsis

`\buf-edit [src-buf] [dst-buf]`
              
## Description

The `\buf-edit` command is used to edit the contents of a SQL buffer
with an external text editor.  The external editor can be defined
using the variable `${EDITOR}` or `${VISUAL}`. If neither of these variables
is defined, then an appropriate text editor is chosen based upon the
operating system in which jsqsh is executing. Specifically, windows
platforms will attempt to execute `notepad.exe` while other platforms
will attempt to run `vi`.
 
## Aliases 

By default the `\buf-edit` command is aliased to `vi` and `emacs`.
           
## Options

### src-buf

Indicates which buffer should be edited. If no src-buf is provided, then 
the current buffer will be edited (`!.`). If the current buffer is empty, 
then the previous buffer will be used.
             
### dst-buf

Indicates in which buffer the edited SQL should be placed. If no destination 
is provided then the current buffer (`!.`) is chosen.
 
## See also

[[\buf-append|\buf_append|buf-append|\buf_append]], [[\buf-copy|\buf_copy|buf-copy|\buf_copy]], [[buffers]]
