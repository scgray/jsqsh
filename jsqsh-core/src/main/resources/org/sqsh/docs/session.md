## Synopsis

`\session [session_id]`
              
## Description

If no arguments are provided then a list of currently active
sessions is displayed, otherwise the session id provided is
switched to.
           
## Options

None

## Examples

When you establish a connection in jsqsh, you have started a _session_, for example:

    1> \connect db2
    Password: *******
    [mydb2.mydomain.com][gray] 1> 

I am now working in a session with the "db2" named connection.  However, jsqsh supports the ability to work with multiple concurrent sessions using the **-n** (or **--new-session**) flag to **\\connect**, like so:

    1> \connect -n bigsql
    Password: *******
    Current session: 2 (jdbc:bigsql://mybigsql.mydomain.com:7052/default)
    [mybigsql.mydomain.com][gray] 1> 
   
And you can view your currently active sessions with the **\\session** command:

    [mybigsql.mydomain.com][gray] 1> \session
    +-----+----------+--------------------------------------------------+
    | Id  | Username | URL                                              |
    +-----+----------+--------------------------------------------------+
    |   1 | gray     | jdbc:db2://mydb2.mydomain.com:51000/MYDB         |
    | * 2 | gray     | jdbc:bigsql://mybigsql.mydomain.com:7052/default |
    +-----+----------+--------------------------------------------------+

And you can switch between sessions using the **\\session** command as well

    [mybigsql.mydomain.com][gray] 1> \session 1
    Current session: 1 (jdbc:db2://mydb2.mydomain.com:51000/MYDB)

and you can end a session (closing the connection) without quitting jsqsh with the **\\end** command:

    [mydb2.mydomain.com][gray] 1> \end
    Current session: 2 (jdbc:bigsql://mybigsql.mydomain.com:7052/default)
    [mybigsql.mydomain.com][gray] 1> 

## See also

[[\connect|connect]], [[\end|end]]
