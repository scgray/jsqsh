## Variable

   `style` - Controls the output display style.
   
## Description

   The `${style}` variable can be set to switch how jsqsh displays its results
   to the user.  This variable is a little different from most jsqsh variables,
   please read the NOTES section, below carefully.

## SQL styles

   The following styles may be set for a SQL (JDBC) session established
   using `\connect`, or may be set when no session has been started.
   
   * `count`  
     The `count` display style is used primarily for performance testing. It
     acts like the `discard` output style in that it fetches all rows, but does
     not format or display them, however it periodically reports the rate of
     row retrievals.  The update interval can be controlled by the ${[[count_report]])
     configuration variable (by default, every 10,000) rows. For example:

        1> select * from some_big_table
        2> go -m count
        Result set #1 returned 10000 rows (10.195s, 980.87 rows/sec), 10000 total (10.195s, 980.872977 rows/sec)
        Result set #1 returned 10000 rows (12.369s, 808.47 rows/sec), 20000 total (22.566s, 886.289108 rows/sec)
        Result set #1 returned 10000 rows (11.567s, 864.53 rows/sec), 30000 total (34.133s, 878.914833 rows/sec)
        ...

   * `csv`  
     Displays the output as a set of comma separated values suitable for 
     loading into, say, Excel. For example:

        1> select DB_ID, OWNER, TBL_NAME from TBLS LIMIT 3
        2> go -m csv
        DB_ID,OWNER,TBL_NAME
        1,gray,struct_simple1
        1,gray,struct_nested3
        81,gray,dist_inventory_fact

     When displaying the output:
        
     * Unless headers are suppressed with the `${headers}` variable or
       via the `--no-headers` (`-H`) option to `\go`, the first row in the
       file will contain the column headers.
     * A NULL value is represented by a completely empty field (this is
       may be changed with the `${csv_null}` configuration variable)
     * A zero-length string is quoted to differentate it from NULL (unless
       a specific NULL representation has been set with $[[{csv_null}]])
     * A value will be wrapped in quotes (which are double-quotes (") by default
       but can be changed with `${csv_quote}`) if the value contains:
       * The quoting character (in which case the quoting character will be 
         escaped (see `${cvs_quote_esc}` for details)
       * Leading or trailing white space
       * The delimiter character
       * A newline

     The following jsqsh configuration variables can be used to alter
     the behavior of the `csv` display output:

     * ${[[csv_delimiter]]}  
       Controls the delimiter used between columns
     * ${[[csv_quote]]}  
       Controls which character or string is used to quote the value of a column
     * ${[[csv_quote_esc]]}  
       Controls how a column containing the quoting
       character is escaped
     * ${[[csv_null]]}  
       Controls how NULL values are represented in the final output

     Each of these variables has its own dedicated help page for more details.
     The variables may either be explicitly set via the [[\set|set]] command, or
     they can be specified at the time the query is executed via the [[\go|go]]
     command, like so:

          1> values('"Hello Scott!", she said', cast(null as varchar(3)), 'c')
          2> \go -H -m csv --var csv_delimiter='|' --var csv_quote_esc=\\ --var csv_null=NULL
          "\"Hello Scott!\", she said",NULL,c

   * `discard`  
      This causes all rows to be retrieved from the query but to
      be discarded (i.e. not displayed). This is primarily useful only
      for timing queries that return rows where you want to discount 
      (most of) the overhead involved in formatting and displaying the
      final rows (this overhead can be quite high for some display formats!).

   * `graphical`  
      Displays the results in a graphical window (GUI). This will
      only work on environments where the graphical interface is 
      available to the JVM. 
        
      Note: the variable `${window_size}` can be used to control the size
      of the window that is opened to display results.
   
   * `graph`   
     A synonym for `graphical`.     
   
   * `isql`  
     This formats its output in a fashion similar to that of Sybase's (or 
     Microsoft SQL Server's) `isql` program.  For example:

        1> select DB_ID, OWNER, TBL_NAME from TBLS LIMIT 3
        2> go -m isql
         DB_ID
            OWNER
            TBL_NAME
         ----------------------
            -----------------------------------------------
            -----------------------------------------------
                              1
            gray
            struct_simple1
                              1
            gray
            struct_nested3
                             81
            gray
            dist_inventory_fact

   * `json`
     Displays results as an array of JSON records (http://json.org). For
     example:

        1> select DB_ID, OWNER, TBL_NAME from TBLS LIMIT 3
        2> go -m json
        [
           {
              "DB_ID": 1,
              "OWNER": "gray",
              "TBL_NAME": "struct_simple1"
           },
           {
              "DB_ID": 1,
              "OWNER": "gray",
              "TBL_NAME": "struct_nested3"
           },
           {
              "DB_ID": 81,
              "OWNER": "gray",
              "TBL_NAME": "dist_inventory_fact"
           }
        ]
   
   * `perfect`  
     This is the default display style and is very similar to how tools like 
     the mySQL client display their output. The output is contained in a 
     text-based grid, like so:
              
        +----------+----------+
        | COLUMN 1 | COLUMN 2 |
        +----------+----------+
        |     1234 | hello    |
        +----------+----------+
              
     The `perfect` display style is so called because it holds rows 
     in memory prior to display, then analyzes the maximum amount of
     data to be displayed in each column and attempts to adjust the
     display accordingly so that as much data as possible fits cleanly
     on the display. To avoid consuming too much memory, the variable 
     `${perfect_sample_rows}` is used to determine how many rows are 
     sampled before the display  process begins--after which point 
     no more rows are held in memory and results are streamed to the
     display as they arrive. For example:
        
        \set perfect_sample_rows=20
            
     indicates that the first 20 rows of the results will be held in
     memory for analysis before display begins. Setting this variable
     to a value less than one will cause all rows to be sampled.
   
   * `pretty`  
     This is visually identical to `perfect` except that it does not
     attempt to perform a perfect fit on the data before display and,
     thus, does not need to hold the results in memory during display.
     Use this style when you need to work with very large result sets.

   * `simple`
     The `simple` style is a simpler form of `pretty`. With it, the
     outer borders are dropped providing a simpler, tighter output
     like so:

        COLUMN 1 | COLUMN 2
        ---------+---------
            1234 | hello

   * `tight`
     The `tight` style is the same as `simple` except that it follows
     the same logic as `perfect` to try to "perfect" the space
     consumed on the final output.  Just like perfect, it consumes
     memory buffering sample rows (dictated by [[perfect_sample_rows]])
     in its attempt to maximize screen real estate.

   * `vertical`  
     The vert (or vertical) style rotates the output, so that every 
     line is represented by a column name followed by a column value. This 
     is nice for looking at particularly wide output.  For example:

        1> select DB_ID, OWNER, TBL_NAME from TBLS LIMIT 3
        2> go -m vertical
        DB_ID:    1
        OWNER:    gray
        TBL_NAME: struct_simple1

        DB_ID:    1
        OWNER:    gray
        TBL_NAME: struct_nested3

        DB_ID:    81
        OWNER:    gray
        TBL_NAME: dist_inventory_fact
   
   * `vert`  
     A synonym for `vertical`.
   
   * `tree`  
     Displays the results in a graphical window (GUI).  Just like graphical 
     except it  displays the resuts in a tree syle format. 

     Note: the variable `${window_size}` can be used to control the size
     of the window that is opened to display results.
        
## Notes

   This variable is a little odd in that it has slightly different
   behavior in different contexts:
   
   * If no session/connection is established, then the style name provided
     must be a valid SQL display style.
   * If a session is established, then the style name must be a valid
     display style for that type of session.
   
This means that you cannot use `${style}` to set the display style of a non-
   SQL session type before the session is started, and when the session ends
   the value of `${style}` will revert to the current SQL display style.

## See also

   [[maxrows]], [[maxlen]], [[perfect_sample_rows]]
