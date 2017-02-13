## Synopsis

`\alias [-G] [name=text]`
              
## Description

An alias in jsqsh is slightly different (and potentially more dangerous
if misused) than the UNIX shell equivalent. In jsqsh, an alias defines a 
piece of text that, when encountered in a user's input, is replaced with
another piece of text. 
   
In its simplest form, it is typically used to allow you to rename commands
so that you can refer to them the way you want to. For example:
   
    1> \alias exit='\quit'
       
will allow you to type:
   
    1> exit
       
to quit jsqsh (note single quotes around the `\quit`. The single quotes
prevent jsqsh from trying to interpret and remove the back-slash).
   
Another common use of `\alias` may be to provide pre-argumented commands.
For example:
   
    1> \alias '\prod'='\connect -U sa -S prod-db -d mssql-jtds'
      
will allow you to later connect to the production database with:
   
    1> \prod-db
    Password: 
      
And, now for the dangerous part. Under normal circumstances aliases only
apply to the first word encountered on an input line. That is, with
the input:
   
    1> \alias one=1
    1> select 'one';
   
the results of the select statement would be the word 'one'. However,
supplying the `-g` flag to alias causes the word to be replace ANYWHERE
it appears in the line of input. so:
   
    1> \alias -G one=1
    1> select 'one';
     
would return a result set with '1' in it. 
   
Why is this so dangerous? Well, it means that you can do silly things
like:
   
    1> \alias select='shutdown with no wait select'
     
I'll leave it as an excersize for the reader to figure out what this
would do (although it would only be effective on Sybase or MSSQL).
     
## Options

### --global (-G)

Causes an alias to be applied globally to a line, anywhere the name of 
the alias appears. Without this flag, the alias will only apply if it 
is the first word on the line.
