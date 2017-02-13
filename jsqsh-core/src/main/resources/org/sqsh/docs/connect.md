## Synopsis

`\connect [options] [connection-name]`
              
## Description

The `\connect` command attempts to establish a JDBC connection to a database 
server.  If a connection is successfully established then the connection 
becomes the active connection for the current session. If the session already 
had a connection established, then the existing connection is closed and 
replaced with the new connection.
              
## Connection options

The following options may be used to specify information required to establish 
a database connection (or whatever your chosen JDBC driver considers a 
connection).
  
In the help description below, you will see references to variables like
`${db}` or `${server}`. When an option says that it sets a given variable,
what is referring to is that if you look at a driver definition using the
command [[\drivers|drivers]], like so:
   
    1> \drivers
    +-----------------+-------------+--------------------------------
    | Target          | Name        | URL                            
    +-----------------+-------------+--------------------------------
    | * MySQL         | mysql       | jdbc:mysql://${server}:${port} 
   
The option may set one of these variables. Not all variables are meaningful
or used by all drivers.

### --setup (-e)

Enters the connection setup wizard. This allow you to easily define named
connections, so that only the connection name needs to be provided to
establish a connection.

### --driver=*driver* (-d *driver*)

The name of the jsqsh driver definition that will be used for the 
connection (use `drivers` to see a list).

### --server=*server* (-S *server*)
  
Sets the value of `${server}`

###  --port=*port* (-p *port*)

Sets the `${port}` property. This typically defines the listening port 
of the server you wish to connect to. Most drivers will provide a 
default value for this property if it is not supplied.
                          
### --database=*db* (-D *db*)

Sets the `${db}` property. This will cause the database context to be set 
to the provided database following connection to the server (in the JDBC 
world the database context is known as the "catalog"). Not all drivers 
support this behavior.

### --username=*user* (-U *user*)

Sets the `${user}` property that determines the username that will be 
utilized to connect to the database.
                          
### --password=*pass* (-P *pass*)

Sets the ${password} property that will be used to authenticate with the 
server.
                          
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

Allows a JDBC URL variable to be set by name. For certain variables explicit
command line options are available to set a variable, for example ${db} can
be set with the `--database` (`-D`) option, other variables may no available
command line option to represent the variable and this option may be used
to set it.  For example, given a driver called `mydriver` with the following
URL:

    jdbc:mydriver:${db};sessionTimeout=$(timeout}

you could connect using:

    \connect --driver mydriver --database mydb --url-var timeout=10

or even:

    \connect --driver mydriver --url-var db=mydb --url-var timeout=10

or, if you prefer the short form:

    \connect -d mydriver -V db=mydb -V timeout=10

### --new-session (-n)

Create a new session for this connect without closing the connection 
in the current session. You can manage, view, and switch active sessions 
with the [[\session|session]] command.
                      
### connection-name

If provided, this indicates the logical connection name that should be 
used to establish the connection. If additional arguments are provided, 
then they will override the same options associated with the logical 
connection name. See **Named connections** below for details.
                      
## Named connection options

Because many options may be required to establish a connection, it can be
quite tedious to provide these options each and every time you connect,
so jsqsh provides the ability to establish a logical name for a given set
of connection properties, and then use this name instead of providing
all of the connection properties. Such named connections can be created
and managed via the Connection Setup Wizard (using the [[\setup|setup]] command)
or more manually via the following options.  

The **NAMED CONNECTIONS** section, below, provides examples of using these
options.
   
### --list (-l)

Lists all available logical connection names. See **Named connections**
below for details.
                      
### --add=*name* (-a *name*)

This option is used to define (or replace) a logical connection name. 
See **Named connections** below for details.
 
### --update (-R)

This option is used to merge together the settings in an existing name 
connection with any additional options you provide. For example, if your 
password was changed on a given server you can do:
                      
    \connect -Pnewpass --update proddb
                         
where *proddb* is the named connection you wish to update.
                      
### --remove (-r)

This option is used to remove a logical connection name. If this 
option is provided a connection-name must also be provided. See 
**NAMED CONNECTIONS** below for details.

### --show-password (-x)

When used with the --list option, causes the connection password to 
be displayed in clear-text.

## Named connections

Because a relatively significant number of parameters are required to
be passed to \connect in order to establish a connection, jsqsh provides
a relatively simple mechanism by which a "name" can be associated with
the set of parameters required to create the connection.  Later attempts
to connect to the same server can be performed simply by providing the
logical name rather than the full parameter set.
   
For example:
   
    1> \connect -Usa -Ppassword -Sprod -p5000 -dsybase --add=PROD
      
will create a connection to a Sybase server (on host prod, port 5000 as sa)
and associate the name 'PROD' with this set of parameters.
   
Later attempts to connect to the same server can be accomplished simply
by providing same name back to \connect:
   
    1> \connect PROD
      
Any of the parameters that were associated with the logical connection
may be overridden using normal command line parameters. For example:
   
    1> \connect -Usgray PROD
      
would cause the connection to be established as 'sgray' (rather than 'sa'
which was original defined).
   
And similarly, persistent edits can be made to an existing named connection
by re-adding the definition along with the change, like so:
   
    1> \connect --add=PROD -Usgray PROD
      
and in the same fashion, a copy with edits can be made:
   
    1> \connect --add=PROD2 -Usgray PROD
      
To view the set of logical connection names currently being managed, you
an use the `--list` argument:
   
    1> \connect --list
    +------+--------+--------+------+----------+----------+--------
    | Name | Driver | Server | Port | Username | Password | ...
    +------+--------+--------+------+----------+----------+--------
    | PROD | sybase | prod   | 5000 | sa       | *******  | ...
    +------+--------+--------+------+----------+----------+--------
   
To delete a logical connection name, use the --remove argument:
   
    1> \connect --remove PROD
      
 Logical connection names are persistent (they exist between executions
 of jsqsh) and are defined in the file `$HOME/.jsqsh/connections.xml`.
   
## Default values

For any of the variables set by options described in the OPTIONS 
section, above, you may provide a default for that variable by
prefixing the variable name with "dflt_" in your session. For example:
   
    \set dflt_port=5555
      
would cause default value of `${port}` to be 5555 unless the `-p` option
is provided to override that value.
   
## Variables set

After a connection is successfully established, the variables described
in the OPTIONS section will be available privately to your session. 
Thus, the variable $port would expand to be the port that was used
when connecting to your session.

## See also

[[\set|set]], [[\drivers|drivers]]
