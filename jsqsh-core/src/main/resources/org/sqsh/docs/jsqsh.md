## Synopsis

```
jsqsh [options] [connection-name]
   -A, --autoconnect          Allows jsqsh to automatically attempt to connect
   -C, --config-dir=dir       Configuration directory in addition to $HOME/.jsqsh.
   -D, --database=db          Database (catalog) context to use upon connection
   -b, --debug=class          Turn on debugging for a java class or package
   -w, --domain=domain        Windows domain to be used for authentication
   -d, --driver=driver        Name of jsqsh driver to be used for connection
   -R, --drivers=file         Specifies additional drivers.xml files to be loaded
   -e, --echo                 Echoes all input back. Useful for running scripts.
   -X, --exit=exit-type       Determines how exit status is computed ("total" failures or "last" failure
   -g, --gui                  Send all command output to a graphical window
   -h, --help                 Display help for command line usage
   -i, --input-file=file      Name of file to read as input. This option may be repeated
   -c, --jdbc-class=driver    JDBC driver class to utilize
   -u, --jdbc-url=url         JDBC url to use for connection
   -n, --non-interactive      Disables recording of input history, and line editing functionality
   -o, --output-file=file     Name of file send output instead of stdout
   -P, --password=pass        Password utilized for connection
   -p, --port=port            Listen port for the server to connect to
   -O, --prop=name=val        Set a driver connection property. Can be used more than once
   -r, --readline=method      Readline method (readline,editline,getline,jline,purejava)
   -S, --server=server        Name of the database server to connect to
   -z, --setup                Enters jsqsh connection setup wizard
   -t, --topic=topic          Displays detailed help on specific topics
   -V, --url-var=name=val     Set a driver URL variable. Can be used more than once
   -U, --user=user            Username utilized for connection
   -v, --var=name=value       Sets a jsqsh variable. This option may be repeated
   -W, --width=cols           Sets the display width of output
```

## Description

JSqsh (pronounced "jay-skwish") is a command line tool primarily intended
for working with relational databases systems.  It provides all of the
basic functionality of any database query too, such as entering and executing
queries, displaying results in a variety of formats, in addition to a number of
advanced features, such as command line editing, history recall, piping of output
to other commands, I/O redirection and much more.

## Setup wizard

In the following sections, you will find a lot of options you may specify
to connect to a server (username, password, host, port, driver, etc.). To
avoid having to provide all of these options, jsqsh allow you to define a
named connection (or alias) for a set of connection properties and provides
a wizard for creating named connections.
   
The `--setup` command line option (or `-S`) starts jsqsh's connection setup
wizard:

    $ jsqsh --setup

this will take you through a series of steps to manage your named connections.
Once the connection wizard is complete you can use a named connection either
by providing the connection name to jsqsh on startup:
       
    $ jsqsh mydb

or you can provide the name to the jsqsh `\connect` command at the jsqsh prompt:
       
    1> \connect mydb

Examples of working with the jsqsh setup Wizard may be found on the [jsqsh wiki
page] (https://github.com/scgray/jsqsh/wiki/Getting-Started#connection-setup).
   
## Connection options

The following options are all available on the jsqsh command line as well as
to its `\connect` command (see [[\connect|connect]]), which may issue from the jsqsh
prompt. The presence of any of these options on the command line will cause
jsqsh to automatically attempt to connect using the information provided, as
if you had executed the `\connect` command.
   
In the help description below, you will see references to variables like
`${db}` or `${server}`. When an option says that it sets a given variable,
what is referring to is that if you look at a driver definition using the
command `\drivers`, like so:
   
    1> \drivers
    +-----------------+-------------+--------------------------------
    | Target          | Name        | URL                            
    +-----------------+-------------+--------------------------------
    | * MySQL         | mysql       | jdbc:mysql://${server}:${port} 
   
The option may set one of these variables and will be mentioned in the help
text below.  For example, the `--server` option sets the `${server}` variable
in the JDBC URL.

### --driver=*driver* (-d *driver*)

The name of the jsqsh driver definition that will be used for the connection 
(use `\drivers` to see a list).
              
### --server=*server* (-S *server*)

Sets the `${server}` JDBC URL variable, which typically specifies the server
name to which you wish to connect.
                         
### --port=*port* (-p *port*)

Sets the `${port}` variable, which typically is the port on which the
server is listening on for connections. Note that if not provided, most
JDBC drivers defined within jsqsh will automatically select a default 
port that is suitable for a default installation of the server to which 
you are connecting.
                          
### --database=*db* (-D *db*)

Sets the ${db} property. This will cause the database context to be 
set to the provided database following connection to the server (in the 
JDBC world the database context is known as the "catalog"). 
Not all drivers support this behavior.
                          
### --username=*user* (-U *user*)

Sets the `${user}` property that determines the username that will be 
utilized to connect to the database.
                          
### --password=*pass* (-P *pass*)

Sets the `${password}` property that will be used to authenticate with 
the server.

### --jdbc-url=*url* (-u *url*)

This option allows you to provide an explicit JDBC URL to utilize when 
establishing the connection rather than using jsqsh's driver mechanism. 
Typically use of this parameter also requires the use of the `--jdbc-class`
argument as well.
               
### --jdbc-class=*class* (-c *class*)

When utilizing a direct JDBC url (and not a jsqsh driver definition), this 
should be utilized to define which JDBC driver class will be utilized for 
the connection.
                          
### --domain=*domain* (-w *domain*)

For Windows based authentication, sets the `${domain}` property, specifying 
the windows domain to be utilized when establishing the connection. Not 
all JDBC drivers support this functionality.
                       
### --prop=*name=value* (-O *name=value*)

This option allows JDBC driver properties to be explicitly set prior to 
connecting, and may be listed more than once. For example:
                      
    \connect --prop compress=true --prop failover=false
                        
The set of properties that are available are specific to a given JDBC 
driver, so you'll need to consult the documentation for your driver for 
details.

### --url-var=*name=value* (-V *name=value*)

  Allows a JDBC URL variable to be set by name. For certain variables, explicit
  command line options are available to set a variable, for example ${db} can
  be set with the `--database` (`-D`) option, other variables may not have an
  available command line option so the `--url-var` option may be the only way of
  setting it. For example, given a driver called `mydriver` with the following
  URL:

        jdbc:mydriver:${db};sessionTimeout=$(timeout}

  you could connect using:

        jsqsh --driver mydriver --database mydb --url-var timeout=10

  or even:

        jsqsh --driver mydriver --url-var db=mydb --url-var timeout=10

  or, if you prefer the short form:

        jsqsh -d mydriver -V db=mydb -V timeout=10
                          
### connection-name 

If provided, this indicates the logical connection name that should be 
used to establish the connection. If additional arguments are provided, 
then they will override the same options associated with the logical 
connection name. See "\help connect" for details on named connections.

## General options

The following command line options are supported by jsqsh.
   
### --config-dir=*dir* (-C *dir*)

The `--config-dir` option can be used to specify a directory containing
jsqsh configuration files (see START UP, below). This directory will
be processed in addition (and following) `$HOME/.jsqsh`.  This option
may be provided more than once if you wish to read multiple configuration
directories.

### --debug=*class* (-b *class*)

Enable java debugging output for a specific jsqsh java class or package.
All such debugging messages will be issued to stdout of jsqsh
   
### --echo (-e)

Causes all input to be echoed to the screen. This is particularly useful
with the `--input-file` (`-i`) option, if you wish to see the supplied
input along with the output of the commands. For example:

    $ cat query.sql
    select count(*) from foo;
    $ jsqsh -e -i query.sql mydb
    select count(*) from foo;
    +----+----+
    | c1 | c2 |
    +----+----+
    |  1 |  a |
    +----+----+
    1 rows in results(first row: 0.191s; total: 0.194s)
    $

### --help (-h)

Display command line help information and exit.
   
### --input-file=*filename* (-i *filename*)

Indicates that input will be provided from an input file rather than
reading from interactive input from a user. 
       
This option may be provided more than once on the command line, and 
input will be read from all files that are provided in the order 
that they are provided. It is important to note that each input file 
is processed within the same physical jsqsh session. This means that 
configuration options or variables that are set by one input file can 
affect the following input file.
       
If the filename provided is `-` that indicates that the input should
be read interactively from the user (from "stdin"). This option is useful
if you wish to execute a "setup" script before supplying input, like so:
       
    jsqsh -i setup.sql -i -
    ...
    1> 
          
will read and execute the contents of setup.sql, then prompt the user
for interactive input.
       
### --non-interactive (-n)

When input is read from a file (`--input-file`), the input is automatically
considered non-interactive, and the following functionality is disabled:
       
* Display of the welcome banner
* Display of the prompt
* Recording of line editor line history 
* Recording of statement history
         
When input is read from stdin, however, jsqsh cannot detect if the 
input is actual user input or is redirected input from a file, like so:
       
    $ jsqsh < input.sql
           
so the `--non-interactive` flag may be used to force non-interactive behavior 
when it isn't desirable.
       
### --output-file=*filename* (-o *filename*)

Causes output to be redirected to an output file rather than the users
screen. Note that error output continues to go to the screen (stderr).
       
### --drivers=*file* (-R *file*)

The `--drivers` option can be used to explicitly load additional JDBC
driver definition files.  The driver definition file teaches jsqsh
how to load a given JDBC driver without the user having to provide
a full JDBC URL and driver class name.  Once jsqsh is started an 
example template for a driver definition file is placed in your
`$HOME/.jsqsh/drivers.xml`.  This option may be provided more than
once if you wish to load multiple driver definition files.

### --exit=*exit-value* (-X *exit-value*)

The `--exit` flag determines how jsqsh computes its final exit status. The
*exit-value* may be one of the following:

* *total* (default)
  The final exit code is the total number of failures seen during the session.
* *last*
   Indicates that the the exit status should be that of the last executed
   command (an explicit `\quit` command is excluded from consideration).

### --setup

Start's jsqsh directly into the driver/connection setup wizard.  This 
is the same as running the `\setup` command at the jsqsh prompt.

### --topic=*topic* (-t *topic*)

Displays the text of a builtin jsqsh help topic and then exits.  This
is the same as running the `\help` command. For example:

    jsqsh --topic help
    
will display help for the `\help`.

### --var=*name*=*value* (-v *name*=*value*)

The `--var` (`-v`) option may be used to explicitly set or define a 
jsqsh variable. For example, if you wish to enable variable expansion
during SQL execution you can do any one of the following:
          
    $ jsqsh --var expand=true
    $ jsqsh --var=expand=true
    $ jsqsh -vexpand=true
    $ jsqsh -v expand=true
    
This option may be provided more than once to set multiple variables.

### --width=*width* (-W *width*)

For certain display styles, such as `perfect` (see [[style]]), jsqsh will attempt to fit
the columns to the current display width.  The `--width` (`-W`) option allows
you to force the screen width to the specified value.

## Start up

Upon startup, jsqsh performs the following activities:
   
1. If `$HOME/.jsqsh` doesn't exist, it is created and populated with two
   files, `driver.xml` and `sqshrc`. These files are described below.
2. If `$HOME/.jsqsh/drivers.xml` file is present, it is loaded. This file 
   can be used to define JDBC drivers to jsqsh that it does not 
   understand natively. The default `drivers.xml` file is empty, but has 
   lots of comments and examples describing the format.
3. If `$HOME/.jsqsh/sqshrc` is read if present. This file main contain any
   jsqsh command or directive, and is typically used to set default
   configuration variables, classpath settings, aliases, etc.
4. If `$HOME/.jsqsh/history.xml` is present it is loaded. This file contains
   your previous query execution history (see [[\history|history]]).
5. Any additional configuration directories specified with `--config-dir` (`-C`)
   (see below) are processed in the same fashion as steps #2-#4.
6. Any additional driver definition files specified with `--drivers` (`-R`)
   (see below) are loaded.
7. Any extension definitions located in `$JSQSH_HOME/extensions` or 
   `$HOME/.jsqsh/extensions` are imported and loaded, if necessary.
8. If any connection options were provided (see **Connection options*, below)
   an attempt is made to connect with the driver. If this fails, jsqsh
   exits.
9. The jsqsh prompt is presented (in interactive mode) or input is processed
   if `--input-file` (`-i`) was provided.  

## See also

[[\connect|connect]], [[\drivers|drivers]]
