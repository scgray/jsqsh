# Release 3.0-SNAPSHOT

## Deprecated features / changes (PLEASE READ)

* There are no other choices for line editors, JLine is it! That is,
  I have removed support for Readline, Editline, and Getline.  To my
  knowledge no one was using them anyway. This has the following 
  implications:
  * The --readline option has been removed
  * The ${readline} environment variable is removed
* Jsqsh now uses JLine3 for it's line editor. JLine3 has lots of really
  useful features (described below), but it also no longer supports 
  processing of $HOME/.inputrc.  Now, if you want to change the keys
  you use to edit, you'll need to use ${editing_mode}.

## New Features

* JLine3 support added, all other readline implementations removed (!)
* The ${prompt} variable may now include various display attributes in it 
  such as:

        1> \set prompt='${attr.blue}${attr.bold}[$server]${attr.default}[$user] $lineno>'

* The ${multiline_editing} can be used to turn on and off multi-line line
  exiting in JLine3.
* Added `\commit` and `\rollback` commands
* Added new `count` display style
* Added support for "snowflake" as a driver type.
* If $JSQSH_HOME is set (which it will be by the shell script that launches 
  it) and $JSQSH_HOME/config exists, then the directory will be loaded in
  the same fashion as $HOME/.jsqsh, allowing the binary to be packaged up
  with custom connection and driver definitions if desired.
* Now uses slf4j+log4j configuration to shut up drivers that use the library.
* Removed all JNI code embedded in JSqsh itself. These have been replaced
  with native Java API's that have been introduced in later years which
  obveate the need for native code.

# Release 2.3

## New Features

* Extension plugin framework: Extensions to jsqsh can be developed that 
  provide a way to load additional commands and variables into jsqsh at
  runtime (at jsqsh startup time, via the new `\import` command, or when
  specific jdbc drivers are loaded)).  With this feature jsqsh can 
  be dynamically extended with new features that, for example, take 
  advantages of features specific to a particular database implementation.
  Details on extending jsqsh can be found in the 'EXTENDING.md" file
  included with the source code.
* Added ability for `\go` to produce a crosstab of the underlying results,
  like so:

        [null][me] 1> select * from x
        [null][me] 2> go
        +-------+-----------+----------+
        | STATE | DAYOFWEEK |    SALES |
        +-------+-----------+----------+
        | NJ    | Mon       | 14.20000 |
        | NJ    | Tue       | 11.40000 |
        | NJ    | Wed       | 19.30000 |
        | CA    | Mon       |  4.10000 |
        | CA    | Tue       |  8.30000 |
        | CA    | Wed       | 44.20000 |
        | NJ    | Thu       | 17.10000 |
        | AR    | Tue       |  4.30000 |
        +-------+-----------+----------+

        [null][me] 1> select * from x
        [null][me] 2> go --crosstab dayofweek,state,sales
        +-----------+---------+----------+----------+
        | DAYOFWEEK |      AR |       CA |       NJ |
        +-----------+---------+----------+----------+
        | Mon       |  [NULL] |  4.10000 | 14.20000 |
        | Tue       | 4.30000 |  8.30000 | 11.40000 |
        | Wed       |  [NULL] | 44.20000 | 19.30000 |
        | Thu       |  [NULL] |   [NULL] | 17.10000 |
        +-----------+---------+----------+----------+
        (8 rows in results(first row: 0.001s; total: 0.002s)

* Added two new display syles, `simple` and `tight`. These styles are 
  identical in behavior to `pretty` and `perfect` (respectively) except
  that they drop the outer borders allowing for a few more characters of
  available screen space per line, like so:

        [null][me] 1> select * from x
        [null][me] 2> go -m tight
        
        STATE | DAYOFWEEK |    SALES
        ------+-----------+---------
        NJ    | Mon       | 14.20000
        NJ    | Tue       | 11.40000
        NJ    | Wed       | 19.30000
        CA    | Mon       |  4.10000
        CA    | Tue       |  8.30000
        CA    | Wed       | 44.20000
        NJ    | Thu       | 17.10000
        AR    | Tue       |  4.30000

* New `--url-var` (`-V`) option is now available in `\connect` or on the
  jsqsh command line to explicitly set a variable contained in a JDBC
  URL by name, like so:

        \connect -Vdb=myDb -dmydriver

  In addition, the connection and driver manager configuration wizards 
  will automatically detect such variables and allow you to set them.

  This feature makes working with new JDBC URl's that need configuration
  parameters that JSqsh didn't natively recognize much simpler.
* Added new `\read` command to allow you to set variables using input 
  provided by the user
* The `csv` display style may now be configured via the new configuration
  variables `${csv_delimiter}`, `${csv_quote}`, `${csv_quote_esc}`, and
  `${csv_null}`
* The `\go` command can now specify values for jsqsh configuration variables
  the the new `--var` (`-v`) argument. Variables will only be set for the
  duration of the command.
* JDBC driver properties can now include environment variables in their values.

## Bug Fixes

* Fixed a loooong standing bug with jsqsh's inability to connect to Oracle.
* When reading input from a file (-i) jsqsh was not handling multi-byte 
  unicode characters properly.

# Release 2.2

## New Features

* New platform support (Mac OS X X86_64, PowerPC 64bit (BE and LE) and s390x)
* `perfect` display mode will try harder to avoid resizing numeric columns
* `\go` options may now be provided after semicolon e.g. `select * from foo; -m vert`
* The `datetime_fmt` now allows `S` to indicate fractions of a second,
  not just milliseconds, so you can now carry the precision out further.
* Added new `exit_on` configuration variable. This variable indicates
  which commands, should they error, will cause the jsqsh session to
  exit.
* Command infrastructure is no pluggable so you can provide jars that
  add additional commands into jsqsh.
* Query time is now carried out to milliseconds precision
* The visual query timer is now enabled by default ('cause I like it!).
  To turn off use `\set timer=false`
* Upgraded jline to 2.13, and added proper handling of ^C while editing
* Internal documentation has been refreshed, and now is nicely formatted
  to the current display width and using bold and underscore. The 
  online wiki documentation is now auto-generated using the `\wiki`
  command.  Check out the awesome new reference documentation at:
  https://github.com/scgray/jsqsh/wiki/Reference
* The `\help` command now takes a --format argument to select how you want
  the help formatted
* The new ${auto_pager} variable controls whether or not help is 
  automatically send through a pager (e.g. "more")

## Bug Fixes

* Fixed NullPointerException when stored proc output parameters return a 
  NULL value. 
* Fixed nested quotes in `CALL` statements like `CALL MYPROC(?='Scott''s book')`
* Fixed bugs with word wrapping in `perfect` display mode
* Handles failure of the driver to return the value for a column more 
  gracefully (instead of stopping the whole result set, just prints 
  `*ERROR*` in the column.
* NullPointerException when calling a stored procedure in Sybase
* Stop print DB2 warning about "statement succeeded". It is annoying.
* Command line arguments with spaces in the value were not 
  handled properly 
* Got rid of unecessary "*" when prompting for passwords
* Fixed NPE if no password is provided at password prompt

## Misc

* Remove Jaql

# Release 2.1.0

## New Features

* New build system! Jsqsh now builds with maven
* Jsqsh JNI libraries are now built into the jar, plus added support for Power 
  Linux 64bit
* New interactive connection setup wizards! This consists of the following:
  * First time startup of jsqsh will enter the setup wizard
  * The `--setup` startup option can enter the setup wizard
  * `\setup` command can enter the setup wizard
  * `\connect --setup` enters the connection setup wizard
  * `\drivers --setup` enters the driver setup wizard
* Each JDBC driver may provide its own class path. This class path can be
  set via the driver setup wizard, and may include environment variables,
  such as `${ORACLE_HOME}/jdbc`.
* Special support for executing stored procedure with output parameters. 
  See `\help storedprocs` for details.
* Each JDBC driver may now specify identifier normalization rules. For example,
  if an object name like MyTable is normalized to MYTABLE or mytable when 
  stored in the catalogs. This allows the metadata commands like `\show` and
  `\describe` to work smarter.
* Each JDBC driver can optionally specify a query to run to determine the
  current schema for the session.  This allows metadata commands like `\show`
  and `\describe` to work smarter.
* The `!N` history recall now allows negative values, so `!-2` means
  you want to edit query you executed two queries prior.
* New variables `$dflt_driver`, `$dflt_username`, `$dflt_database` available
  to provide defaults for connection arguments that are not supplied.
* Added new `-X,--exit-value` startup flag to indicate to jsqsh what to use
  for its exit status. The default value is `total` (`--exit-value="total"`)
  in which the exist status it the total number of failed jsqsh commands.
  The value `last` indicates that the last executed statement determines
  the exit status
* Added `${max_rowsaffected}` to deal with buggy drivers (Hive) that don't
  properly indicate when they are done sending results to the client.

## Bug Fixes

* Improved the PL/SQL determination of whether or not '`;`' is a statement
  terminator.
* The visual timer is better about clearing itself before results are
  displayed
* JSqsh will try to recognize tabs that come from a cut and paste 
  operation and avoid doing tab completion under those circumstances.
* Connection variables, such as `$server`, will expand correctly now. 
  Previously these would only contain a value if the value was provided 
  explicitly, but when the value was provided explicitly by the driver
  definition, the value would be null.
* Fixed inability to start jsqsh if a driver throws an error (java.lang.Error)
  while being loaded. 
* Fixed handling of Hive DECIMAL type


## Misc

* Debian package is no longer dependent upon readline-java.

# Release 2.0

## New Features

* The command terminator may now end a jsqsh command. For example, the
  following are now legal:

      1> quit;

  or

      1> \echo hello how are you;
      hello how are you

* Added new SQL display style `json`. 
* Added `$histid` variable that contains the current buffer number you are 
  working on. Really useful for adding to your prompt.
* Added support for ARRAY data type and hopefully better support for
  unrecognized data types.
* Added `\show` command which can display almost all of the major database
  metadata information (tables, functions, columns, procedures, etc.).
  See `\help show1 for details.
* Added `\prepare` command. The command is mostly used for diagnosing JDBC
  driver issues/functionality and prepares a SQL statement against the
  server than displays metadata about the prepared statement, such as
  what the final result set will look like and the data types of any
  parameter markers.
* Added `$fetchsize` variable to set the JDBC Statement.fetchSize() property.
* Added `$editing_mode` to allow you to set the editing keymap. Currently
  this is only supported by JLine, so to switch to "vi" mode, do:

      \set editing_mode=vi

* Added `${ex_detail}` to adjust the level of detail in exceptions (e.g.
  whether or not stack traces are included in the output).
* Added `${show_exclass}` configuration variables that causes exception 
  class names to be displayed.
* Added `--echo` jsqsh flag and `${echo}` variable to echo the input back out 
  to stdout. This is useful when running scripts (non-interactive mode)
  see both the SQL and commands executed as well as the results.
* Added `hive` as a pre-defined JDBC source.
* Added `--var` (`-v`) command line flag which can be used to set the value 
  of a variable like so

      jsqsh --var expand=true

  this flag can be provided multiple times so set multiple variables.
* Added `--width` (`-W`) flag and ${width} variable to force the display width to a 
  specific value rather than using the terminal width.
* Added `\globals` command. Right now this only works for a Jaql session,
  and displays all variables defined for the current session.
* The `\connect` command and jsqsh itself now take a `--prop` (`-O`) option that
  can be used to set JDBC driver connection properties. This option may
  be provided more than once to set multiple properties.
* The `\connect` command now provides a `--update` option to update the settings
  on an existing connection.
* Added `--timeout` (`-t`) flag to the `\go` command to specify a timeout period
  (in seconds). 
* Added `$timeout` variable to control query timeout interval (how long to 
  let a query run before automatically stopping it)
* Added `$timer` variable. When set to true, an on-screen duration timer will
  appear while you wait for your query to complete, helping you to keep
  track of how long the query has been running.
* Added `\stack` command to display the full stack trace from the most 
  recently displayed error or exception.
  
## Changed Behavior

* The `$expand` variable now defaults to false, meaning that by default
  variables are no longer expanded in SQL statements. After some thought
  I decided this behavior was too dangerous to leave on by default as 
  a "$" is meaningful on some platforms and could break/confuse things. Note
  that this does not impact variable expansion on jsqsh command line commands, 
  that remains in effect.
* Footer information (#rows processed and timing) is now displayed
  to stderr so that this information isn't written to the output file
  if the output is redirected.  Use `2>&1` if you want to write this to
  your output file now.
* The default display style for timestamp and date have changed
  to the ISO standard of `yyyy-mm-dd hh:mm:ss.fff` and `yyy-mm-dd`
  respectively.
* The behavior of setting `$style` is slightly different now, and is documented
  in the help for style. The support styles vary based upon the session type
  (e.g. a JDBC session vs. a Jaql session). Setting a SQL style with no
  session started, or a SQL session started sets it globally for all session.
  Jaql styles can only be applied during a jaql session, and only sets the
  value locally for the jaql session.
* Upgraded to jline-2.8. JLine is an almost pure java line editing library, 
  similar to readline.  This version of jline adds support for most of the 
  basic "vi" command set. It is important to note that this version of jline
  will recognize and try to honor settings in your ".inputrc" file if you
  have any set.
* (internal) Changed the way in which JDBC drivers are loaded via a custom class
  path (using the `${classpath}` variable).  Apparently some drivers don't
  like to be used directly to establish the connection, so this change
  will use the JDBC DriverManager to get connections.
* Removed support for ANTs DB2 SQL Skin for Sybase ASE driver. The company 
  is dead except for the nails in the coffin. Too bad, I was extremely proud of 
  what we built.
* The `--debug` (`-b`) flag can now be specified multiple times to enable 
  multiple debugging categories
* The `--input-file` (`-i`) flag can now be specified multiple times, each
  input will be processed in order.
* The `\help` command will automatically try to launch a pager to page
  through the help output.
* Output performance for most output formats is dramatically improved (approx 2x)

## Other

* Switched to Apache 2.0 License. This has the following side effects
  (mostly not visible to users)
  * Option processing switched from GNU getopt to Apache CLI
  * JLine is made default line reader. Readline has to be enabled
    by the user, and the readline jar is not distributed with jsqsh.
    For those worried about this descision: java-readline hasn't been
    updated in over 7 years and JLine 2.x is approaching it pretty well
    in terms of functionality.  Java-readline can be installed on most
    linux machines by your package manager, so it is easy to add on.
* Added support for launching a Jaql interpreter from within jsqsh. Jaql 
  is a programming language designed for working with Hadoop. JSqsh's 
  support for jaql consists of:
  * The `\jaql` command starts a Jaql session
  * The `--jaql` when starting jsqsh, automatically starts a session
  * When in a jaql session, `\go` or the command terminator (`;` by default)
    executes the current statement through the Jaql interpreter.
  * The variables `jaql_prompt`, `jaql_indent`, `jaql_jars`, `jaql_path`,
    `jaql_jobname`, and `jaql_style` can be used to affect the behavior 
     of the jaql shell (these are all documented under "\help jaql").
  * Jsqsh options `--jaql-path` and `--jaql-jars` have been added. Use of
    these options implies `--jaql`.
  * Tab completion will complete any global Jaql variable name.  
  * There are three display styles that apply to Jaql: `json`, `lines`,
    and `discard`.
* Added new build target called `dist-bin`. This produces a standalone
  zip package containing a jsqsh run-time, just like `dist`, but it
  also includes the jsqsh JNI interface.
* The shell script included with `dist` and `dist-bin` is now wired
  to recognize if `$JAQL_HOME` is set and to launch jsqsh using the
  Jaql launcher so it will pick up all the necessary libraries.
    
Bug Fixes
* Fixed a long standing irritation of empty entries appearing in the
  `\history` list. Now empty SQL buffers will be thrown away so no
  empty entries should appear in the history.
* Fixed bug where connection names supplied on the command line
  are not recognized.

# Release 1.4

* Added support for "JLine" as an input method. JLine is a pure-java
  (at least on UNIX) library similar to Java-Readline. For portability,
  java-readline will be tried first, if it is unavailable, JLine will
  be used.  This support added the following:
  * `--readline` command line option to specify the input method.
  * `JSQSH_READLINE` environment variable can also be used to specify
    the input method.
  * The `$readline` variable can be used to display the currently
    selected readline implementation.
* Added new options to the `\insert` command:
   * `-t`,`--terminator=term`  Allows you to specify a batch terminator
     other than `go`.
   * `-m`,`--multi-row` Enables support for platforms that allow multiple
     rows to be specified in a single insert statement.
* Added `-n` (`--repeat`) option to `\eval` allowing the batch of SQL being
  evaluated to be run multiple times.  Each iteration of evaluation
  will have a variable ${iteration} set that it can refer to if it
  wants to vary the SQL behavior by iteration.
* Added new command line option `--config-dir` that allows you to specify
  additional configuration directories to be processed upon start
  in addition to `$HOME/.jsqsh`. This option may be specified more than
  once to load more than one configuration directory.
* Added new command line option `--drivers` that allows you to specify
  additional `driver.xml` files to be loaded upon startup. This option
  may be specified more than once.
* Added `--load` option to `\drivers` command to allow you to load additional
  `driver.xml` definition files. This is particularly useful to put into
  your `.jsqsh/sqshrc` file.
* Added `isql` display style, that attempts to format data in a fashion
  similar to Sybase's isql program.
* Added `vertical` (or `vert`) display style, that attempts to format 
  in a rotated "name: value" style.
* Added documentation for the `${rc}` variable. This variable contains
  the return code from the most recently executed jsqsh command, which
  will be 0 for success or non-zero for failure.  For example. the 
  `\go` command returns a non-zero value if the SQL executed failed.
* Added `${fail_count}` variable that contains the total number of
  commands that have been executed within the session that have failed
  (i.e. `${rc}` from the command was non-zero).
* Jsqsh will now exit with a value indicating the total number of failed
  commands (which is the sum total of `${fail_count}` from each session
  that was created within jsqsh). 
* Added `${binstr_fmt}`, a boolean variable, that controls whether or 
  not binary values are displayed as raw binary values (false=0xabcd) 
  or as binary string values (true=X'abcd').
* Added `${exec_mode}` variable that can change the manner in which 
  jsqsh executes SQL against the remote server. This can be one of
  "immediate" (the default) or "prepare".
* Added driver name 'db2zos'. This is identical to the "db2" driver
  except that it sets the "emulateParameterMetaDataForZCalls" property
  and sets the execution mode to "prepare" upon connection (DB2 z/OS
  can execute "call procname()" calls if the SQL is prepared first).
* Added `&lt;SessionVariable&gt;` property in the `drivers.xml` that allows a 
  driver to set certain session variable values upon connection.
* Added `--show-password` option to `\connect --list` because I keep
  forgetting passwords to systems.
* Added sql-skin driver definition for the IBM DB2 SQL Skin for ASE
  driver (I happen to work for them, so have a bit of incentive).
* The `perfect` display style no longer holds all of the rows in
  memory prior to display.  Instead, it is now controlled by a new
  variable `${perfect_sample_rows}` (default value 500). So now, 
  `${perfect_sample_rows}` rows are held in memory for analysis prior
  to display. Once this limit has been hit, rows are streamed and
  no longer held in memory.
* The `${maxrows}` variable now defaults to 0 (unlimited rows) rather
  than 500. This is done because the default display style (`perfect`)
  no longer attempts to hold all rows in memory.
* Added driver definition for the H2 JDBC driver.
* More care is taken to ensure that database connections are actually
  formally close()'d when jsqsh exits or a session terminates.
* When in non-interactive mode, the warning about failures to load the
  jsqsh JNI interface is suppressed.
* When connection arguments are provided on the command line and a connect
  cannot be established, jsqsh will now exit with an error instead of
  dropping you into a prompt.

# Release 1.3

* The `dist-rpm` target and has been updated so that it works on 
  more recent RPM distributions (specifically I am running FC10).
* Compilation requires JDK 1.6 now due to support for new datatypes. It
  should run OK in JDK 1.5 provided you don't reference one of those
  datatypes.
* A driver definition (the `drivers.xml`) file may now contain a new
  `&lt;Property&gt;` tag to define properties that are to be passed into the
  driver at connect time.
* The DB2 driver now sets the `retrieveMessagesFromServerOnGetMessage`
  property. This results in actual usable error messages.
* The `\call` statement is now capable of displaying Oracle result sets
  produced from Oracle refcursors. This is done with the special ":r"
  argument placeholder.  See "\help call" for details.
* The `perfect` display mode will now attempt to compact the display columns
  sufficiently tight to fit within the screen (only if jsqsh JNI is 
  available).
* Added mssql2k5 driver definition for the SQL Server 2005 JDBC
  driver.
* History buffer numbers are now numbered oldest to newest, so that 
  the buffer number for a given buffer will not change until the 
  session ends or the buffer is removed from history.
* Added `--repeat` to `\go`, so that a query can be executed multiple times.
  During this execution the variable ${iteration} can be referred to
  within the query to determine which iteration of the repeat cycle
  the query is on (starting from 0).
* Added display style of `discard` that discards all rows. This is useful
  for performance testing.
* Added `${scale}` and changed the definition of `${precision}`. These
  two values now work together when displaying floating point
  values exactly as one would expect them to.
* Added `${querytime}` configuration property to suppress display of
  query timing information. 
* Added `dist-sparc-sol` build target. This provides a `.tar.gz` file
  that contains jsqsh and its JNI files. Currently this relies upon
  the build platform having readline-5.x installed
* Added support for SQLXML datatype
* Enabled support for "N" types, `NVARCHAR()`, `NCHAR()`, etc. 
* Fixed recognition of Oracle `NUMBER` type when it is not qualified with
  a specific precision and scale.
* Fixed case where stored procedures with multiple result sets would
  occasionally not show all of the available result sets. Unfortunately,
  this problem still persists in the SQL Server 2005 driver, but I think
  its a bug in the driver.
* Fixed persistence of the SID attribute of a connection to the 
  connections.xml file. 
* Fixed NullPointerException when maxrows_method=driver 
* Fixed `$user` not being set when the username is provided at the 
  "Username:" prompt rather than on the command line.

# Release 1.2

* Added new mechanism to `\connect` to allow a logical name to be assigned
  to a connection and persistently saved away so that the connection
  can be re-established in the future using only the name. This provides
  a cleaner mechanism for managing connections than the 'alias' command.
  Run  `\help connect` for more details.
* Added session-to-session redirection, allowing the output of a 
  command executed in one session to be executed by another session
  (or even re-executed by itself).  See `\help redirection` for details.
* The `\call` command can now iterate through a CSV file and execute a
  prepared statement once for each line in the file, using the fields
  in the file as arguments to the procedure.
* Fixed handling of `bit` datatypes.
* Fixed ^C handling that I broke the last release.
* Fixed the `\eval` command aborting when it executes a command that
  causes a session switch, such as `\session`  or `\connect -n`.
* Fixed `\connect -c` not finding classes that are specified in the
  `${classpath}` jsqsh variable.
* Fixed `OutOfMemoryError` when processing the results of a backtick
  expression (e.g. \set x=\`echo 1234\`)

# Release 1.1

* Changed several aspects of command line options:
  * All jsqsh options have a long form and a short form 
    (e.g. `-h` and `--help`).
  * You no longer need a space following an option (e.g. 
   `-U sa` and `-Usa` are equivalent).
  * Every jsqsh command accepts `-h` and `--help` to cause a usage
    of the command to be dumped.
  * Every jsqsh command accepts `-g` and `--gui` to cause its output
    to be sent to a graphical popup window.
  * The new help topic "options" discusses this.
* Added new display style `graphical` that will pop open a GUI window
  containing query results in a sortable graphical table.
* Added `$window_size` to control the size of the window in graphical
  display mode and $font to set the font that will be used.
* Added `$nocount` variable to turn off the display of rowcounts generated
  due to update/delete statements.
* Added `\insert` command, this displays the results of the current SQL
  query as INSERT statements or executes the INSERT statements in 
  another session.
* Added `\select` command to generate select statements and joins from
  table definitions.
* Added `\create` command to generate CREATE TABLE statement from a table
  definition.
* Added `\diff` command to compare results from SQL executed across 
  multiple session.
* Added `\call` command to execute a prepared statement. This command
  is primarily for debugging purposes.
* The `\help` command is now more organized.
* JSqsh is now better at determining if the session is interactive
  or non-interactive (i.e. if stdin has been redirected).
* Tab completion now recognizes the TSQL "use" statement.
* Display of "info" type SQLWarning's (e.g. the output from a "print"
  statement from Sybase and Microsoft) is improved.
* Fixed the `-i` flag causing jsqsh to exit if the script contained
  a `\connect -n`.
* Fixed failure when trying to format Oracle TIMESTAMP columns for
  display.
* Fixed `\connect` command trying to use settings from the current session
  when trying to connect. Read the `\connect` command for details.
* Fixed bug in PL/SQL parser that cause jsqsh to sit in tight loop
  eating CPU when parsing certain PL/SQL blocks.
* Fixed bug with way getUpdateCount() is being called that was causing
  Sybase's jConnect driver to complain.
* Updated driver definition to use the jConnect 6.x driver for Sybase
  
# Release 1.0

* Tab completion now recognizes stored procedure execution requests and
  will attempt to complete parameter names and procedure names.
* Added `-i` flag to GO to allow you to generate INSERT statements from a 
  result set.
* Added support for `CLOB` and `BLOB` datatypes (bug #1820183). Current support
  requires holding the entire object in memory so be careful!
* Added `$show_meta` variable to display JDBC result set metadata as a separate
  result set when executing queries.
* Added `$maxrows_method` variable that is used to control the mechanism that
  jsqsh uses to limit row results. This added because I discovered that 
  setting the row limit in some JDBC drivers caused unwanted side effects.
* Added `\procs` command to display list of procedures in the current database.
* Added PL/SQL analyzer so that semicolon termination can be used against
  Oracle. 
* Added `\debug` command to turn on debugging for specific classes.
* The `\describe` command will now describe stored procedure parameters.
* Improved formatting of number datatypes (tinyint, int, short, bigint,
  float, double, etc.) to minimize the amount of space required for display
  wherever possible.
* Fixed the driver definition for the DB2 JDBC driver.
* Commands that edit or alter the current SQL buffer will also affect the
  readline history as if the user had just typed in the buffer. E.g. if you
  hit '!3' at the prompt to re-load the third oldest query, the readline
  history will look as if you just typed that query. 
* Fixed bug #1816504 - Queries returning multiple result sets were getting
  fouled.
* Fixed bug #1820182 - Handling of Oracle NUMBER columns failing during
  formatting. 

# Release 0.9.5

* Dramatically improved tab completion. It is now contextual to your position
  within the SQL statement being written. If the FROM clause has been written
  or the target of the INSERT/UPDATE/DELETE has been specified, then the
  completion will restrict itself to that context. This was more difficult
  than I expected!
* Added `$headers` and `$footers` configuration variables to control display
  of result header/footer information
* Added `-h` and `-f` to the `\go` command to toggle the display of result 
  header and footer information respectively.
* Added `-w domain` flag to `\connect` so that domain based authentication
  can be used when connecting. Currently only the mssql-jtds driver 
  recognizes this option. 
* Added `\eval` command to read and process the contents of a file.
* Added tab completion of procedure names.
* Added handling/display of SQL warnings.
* Improved formatting of error messages.
* Added testing of JVM support for catching CTRL-C. If it is not supported
  (i.e. the gcj JVM), then a warning message is issued.
* Added support for gij (gcj's JVM) 1.5.0
* Added default aliases `vi` and `emacs` or `\buf-edit`
* Removed `csv` display of footer information (rowcounts).
* By default the `\history` command will now only show the first 10 lines of 
  any SQL statement. The `-a` flag has been added to allow you to see the 
  complete statements if you want.
* Fixed `OutOfMemoryError` when nested SQL exceptions are returned by the server.
* Fixed `NullPointerException` when running non-interactive scripts
  of SQL via the `-i` flag.

# Release 0.9.4

* Added tab completion of object names. The logic is pretty smart too, and
  understands "quoted object names" and [bracketed object names].
* Added `-m style` flag to the `\go` command.
* Added new display style `csv`.
* Added new `${classpath}` configuration variable to allow the user to
  specify additional jar locations for JDBC drivers.
* Added support for empty passwords
* Added new `\macro` command for defining velocimacros
* Fixed documentation for the `\connect` command
* Fixed `-D` flag for the `\connect` command
* Fixed handling of NULL values for binary data types.

# Release 0.9.3

* Initial release. Seems stable enough (I use it every day), but
  warrants some shakedown before 1.0 release
