## Variable

  `prompt` - Defines the query prompt.

## Description

  The `${prompt}` variable defines the prompt that will be displayed before
  each line of input. Unlike other variables, this variable may, itself,
  contain variables that will be expanded prior to display.
   
  For example, the default value:
   
    #if($connected==true)[$server][$user] #end$lineno>
      
  will show:
   
    1>
      
  when not connected to a server and:
   
    [prod-db][sa] 1> 
      
  when connected (the `#if`/`#end` syntax is part of Velocity, the tool
  that is used to expand variables. Documentation is available from
  http://velocity.apache.org/engine/releases/velocity-1.5/user-guide.html).
   
  Care must be taken when setting this variable to ensure that the
  varables are not expanded when it is being set. For example:
   
    1> \set prompt="$lineno>"
      
  will have the $lineno expanded at the time the prompt is set and
  is exactly identical to:
   
    1> \set prompt="1>"
      
  which clearly isn't what you inteded. Instead use single quotes:
   
    1> \set prompt='$lineno>'
   
## See also

  [[\set|set]]
