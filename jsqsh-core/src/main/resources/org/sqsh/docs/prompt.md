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

## Colors and styles

  In addition to the standard variables that jsqsh provides, the
  jsqsh ${prompt} variable may refer to a special ${attr} variable
  that allows you to control a number of text attributes, such as color,
  bolding, underlining, etc. The following text attributes may be
  referenced in the prompt variable:

  * ${attr.bold} - Turns on bold styling
  * ${attr.dim} - Turns on dim (basically turns off bold) styling
  * ${attr.underline} - Turns on underline
  * ${attr.blink} - Turns on blinking
  * ${attr.inverse} - Reverses background and foreground colors
  * ${attr.black} - Makes the foreground text color black
  * ${attr.red} - Makes the foreground text color red
  * ${attr.green} - Makes the foreground text color green
  * ${attr.yellow} - Makes the foreground text color yellow
  * ${attr.blue} - Makes the foreground text color blue
  * ${attr.magenta} - Makes the foreground text color magenta
  * ${attr.cyan} - Makes the foreground text color cyan
  * ${attr.white} - Makes the foreground text color white
  * ${attr.background.black} - Makes the background text color black
  * ${attr.background.red} - Makes the background text color red
  * ${attr.background.green} - Makes the background text color green
  * ${attr.background.yellow} - Makes the background text color yellow
  * ${attr.background.blue} - Makes the background text color blue
  * ${attr.background.magenta} - Makes the background text color magenta
  * ${attr.background.cyan} - Makes the background text color cyan
  * ${attr.background.white} - Makes the background text color white
  * ${attr.default} - Turns off all styling set via the above settings

  Note that not all styles work on all terminals.

  For example, if you want to make the server name in your prompt bold and
  blue, you would do:

        1> \set prompt='${attr.blue}${attr.bold}[$server]${attr.default}[$user] $lineno>

## See also

  [[\set|set]]
