## Command line redirection

As with standard Bourne shell (and most other shells, for that 
matter), a command's output may be redirected using a special
notation interpreted by the jsqsh. The following may appear anywhere
on the command line, but only redirection that is specified prior
to a pipe (`|`) actually has any effect on the behavior of internal
jsqsh commands.
    
* `[n]>file`  

   Causes the output of the command to be redirected to a 
   file.  Following standard shell conventions, the `n` is 
   an optional number that can be used to indicate which
   output of the command is redirected to the file.  A `1`
   indicates the regular output should be redirected, and
   a `2` indicates only the errors should be redirected.
   If no number is provided, then regular output is diverted.
              
   For example:
              
        \go >tmp.out
   
   Sends the output of the `\go` command to a file called 
   `tmp.out`. Errors will still go to your screen.
                            
        \go 1>tmp.out
       
   Synonym for the above.
                            
        \go 2>tmp.out 
       
   The results from the `\go` will go to your screen but errors 
   will be captured in tmp.out.
    
* `[n]>>file`  

   This is identical to the above except that if the file 
   exists, it is appended to rather than overwritten.
    
* `[m]>&[n]`  

   This allows one type of output stream (regular output
   or errors) to be redirected to the other type of stream.
   For example:
              
        \go 2>&1

   Causes errors from the SQL to go to the regular output 
   stream
              
        \echo "Error!" 1>&2 

   Causes the output of the \echo command to be send to the error 
   stream.
                 
        \echo "Error!" >&2 

   A synonym for the above.
   
* `>+[id]`  

  This syntax is specific to jsqsh and is used to redirect the output 
  from the current command to the context of another session (specified 
  by `id`). If no session id is provided, then the output of the current 
  session is "looped back" to itself. See **Session redirection**, below 
  for details.
             
## Pipes and redirection

It is important to understand that jsqsh handles pipes very differently
from that of a typical shell. In jsqsh anything following a pipe 
character (`|`) is passed directly to the operating system shell, which
means that any I/O redirection following the pipe character is being
performed by your local operating system shell and not by jsqsh and will
therefore follow whatever rules are defined by your O/S shell.

## Session redirection

Session redirection is indicated by the special syntax `>+[id]` where
`id` is the id of the session that you wish to re-direct the output to.
If the id is not specified then the output is sent back to your current
session for subsequent processing.
    
When jsqsh encounters a session redirection the following takes place:
    
1. A temporary file is created and the commands output is sent to
   that file.
2. If the command involves a pipe, the output of the pipe is sent to
   that file.
3. After the command completes, the contents of the file is executed
   in the context of the target session as if:
       
        \eval tmp_file
       
   was executed in the target session.
       
The important item to note is that any output from the command being
directed is treated *exactly* as if the user had typed it into the
prompt in the context of the target session.
    
Here's why this is so important to understand:
    
    1> echo 'select 1' >+2 
       
will *not* cause a `select 1` to be executed in session #2. All that
will happen is that jsqsh will switch to the context of session #2,
pretend that the user has typed `select 1`, and then switch back to the
current session.  The end result, is that a `select 1` will be appended
to the current SQL buffer (the SQL buffer is independant of sessions).
    
If you actually want to execute the command then you can do one of two
things:
    
    1> echo 'select 1;' >+ 2
       
or 
    
    1> echo 'select 1' >+2
    2> \echo 'go' >+2
       
In the first case, the semicolon causes the execution of the SQL
and in the second, the `go` executed in the context of session #2
causes the execution.
