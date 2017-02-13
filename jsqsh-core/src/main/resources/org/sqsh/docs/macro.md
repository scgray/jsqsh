## Synopsis

`\macro`
              
## Description

The `\macro` command takes the current buffer that you are typing into and runs 
it through the Velocity engine, with the primary purpose of allowing you to define 
re-usable macros (velocimacros)
   
By way of example:
   
    1> #macro (prod $tab)
    2> use $tab
    3> #end
    4> \macro
    Ok.
    1> #u('master')
    2> \go
      
We have defined a macro called #u() that takes the name of a database
and expands to be  "use" command on that database (granted, this is a
pretty worthless example, but you get the idea).
   
For more details on velocity, see 
[velocimacros](http://velocity.apache.org/engine/releases/velocity-1.7/user-guide.html#Velocimacros)
 
## Options

None
   
## See also

[[buffers]], [[expand]]
