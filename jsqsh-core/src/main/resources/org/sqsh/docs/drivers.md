## Synopsis

`\drivers`
              
## Description

Displays a list of drivers that are currently registered with
jsqsh. An asterisk (*) will be displayed next to drivers for
which the actual JDBC implementation (.jar file) is available.

JSqsh does not come bundled with any JDBC drivers therfore unless
you already have a driver available in your classpath (via the
$CLASSPATH environment variable), you will likely see a list of
drivers without any marked as available:

    1> \drivers
    +--------------------------------------------+-------------+----------
    | Target                                     | Name        | URL      
    +--------------------------------------------+-------------+----------
    |   Starschema BigQuery                      | bigquery    | jdbc:BQDr
    |   IBM Data Server (DB2, Informix, Big SQL) | db2         | jdbc:db2:
    |   IBM DB2 z/OS                             | db2zos      | jdbc:db2:
    |   Apache Derby Server                      | derby       | jdbc:derb
    |   Apache Derby Embedded                    | derbyembed  | jdbc:derb
    ...

(note that the output shown above is truncated to fit this help
text nicely).

Any one of the following mechanisms may be used to make a driver available
to jsqsh:

1. Each driver may individually have it's classpath set via the [[\setup|setup]] 
   command (run `\setup` and navigate to the driver you for wich you wish 
   to provide a classpath).  This is the preferred method for making a driver
   available (see [[Driver setup|Getting-Started#driver-setup]] for details).

2. Setting the ${[[classpath]]} jsqsh variable to a location containing
   JDBC jar(s) for the driver(s) you want.
   
3. Making sure the driver is contained in your CLASSPATH environment
   variable, e.g., for UNIX or cygwin:

        $ CLASSPATH=/path/to/some/jdbc/driver.jar:"$CLASSPATH"
        $ export CLASSPATH

   or, for Windows:
   
        C:\> set CLASSPATH=/path/to/some/jdbc/driver.jar; %CLASSPATH%
        
4. Alternatively, you can add a JDBC driver implementation to jsqsh's
   startup jar directory. The path to this directory can vary from
   platform to platform but can typically be found in one of the
   following locations relative to where the jsqsh binary is located:

        ../lib
        ../share
        ../share/jsqsh

## Adding new drivers

JSqsh comes with a bunch of pre-defined driver definitions built-in,
however you can provide your own additional definitions if you wish
using the jsqsh setup wizard (run `\setup`). The driver definitions
that are created or edited are located in `$HOME/.jsqsh/drivers.xml`.
           
## Options

### --load=*file* (-l *file*)

Loads an additional `drivers.xml` format file.
   
## See also

[[classpath]]
