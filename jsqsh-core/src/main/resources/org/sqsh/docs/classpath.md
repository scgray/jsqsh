## Variable

  `classpath` - Defines additional JDBC driver search locations
   
## Description

  The `${classpath}` variable can be used to specify places in which 
  JDBC drivers can be found on your system. The path is specified
  as a delimited list of ".jar" files or directories containing ".jar"
  files.  The delimiter is `;` on Windows or `:` on UNIX.  The following
  is an example of a valid UNIX classpath:
   
    /home/gray/jars/sybase.jar:/home/gray/drivers/
      
  where the first entry is an explicitly specified jar file (`sybase.jar`)
  and the second entry is the path to a directory that contains multiple
  jars, each of which will become part of the classpath.
   
## See also

  [[\drivers|drivers]]
