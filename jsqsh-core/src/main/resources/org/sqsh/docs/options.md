## Command line options

Jsqsh commands executed on the jsqsh command line follow many 
of the same rules as program executed at your computers command
line (well, unless you are used to working in the Windows 
command shell, then you'll find it is better).
   
All jsqsh commands accept command line options. These options
are provided in two different forms: a short form an a long form.
   
## Short options

Short command line options are provided in the form of a dash (`-`)
followed by a letter. For example, the `\connect` command requires
a username to be used to establish a connection to the database
server.  The short form of this option is `-U` followed by the name
of the user. You can either have a space following the username or 
not, so that:
   
    \connect -Usgray
      
is the same as:
   
    \connect -U sgray

In some cases, the command line option does not take an argument 
(in the above "sgray" was the argument to the `-U` option). For 
example, with the `\echo` command:
   
    \echo -n hello
      
the `-n` flag indicates that a new-line should NOT be displayed after 
printing the word "hello" 
   
## Long options

Short options are easy to enter, but for command, such as \connect,
which require a lot of options, it can begin to look like alphabet
soup:
   
    \connect -Usa -Pguessme -Dmaster -Ssql-prod -dmssql-jtds
   
To address this, every command line option also has an alternate
long form. For example, the equivalent of the `-U` option for 
`\connect` is (note the double-dashes, they indicate it is a long option):

    \connect --user=sgray
   
or
   
    \connect --user sgray
      
this, the full command line shown above would be:
   
    \connect --user=sa --password=guessme --database=master --server=sql-prod --driver=mssql-jtds
   
this form may take more typing but is much more explicit.
   
## Option documentation

In all help documents, options are generally shown as:
   
### --long-name1=*value* (-X *value*)
   
where the option `-X` is also known as `--long-name1` and takes
an argument (e.g. `-X value`, `-Xvalue`, `--long-name1=value`
or `--long-name1 value` are all the same).
   
### --long-name2\[=*value*\] (-Y [*value*])
      
where the option `-Y` is also known as `--long-name2` and has
a value that is not required. 
   
### --long-name3 (-Z)
    
where the option `-Z` is also known as `--long-name3` and does
not have an argument (e.g. `-Z`, or `--long-name3` are the same)
      
## Options supported by all commands

All jsqsh commands accept the following arguments:
   
### --gui (-g)

Display all output in a graphical popup window. This is useful for 
setting aside a result set for later reference while you continue to work.
               
### --help (-h)

Displays a description how to execute the command.
