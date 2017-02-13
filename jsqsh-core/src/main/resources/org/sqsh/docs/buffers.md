## JSqsh SQL buffers
    
Each time you create and execute a SQL statement, that statement is saved
away into your SQL buffer history. The list of prior SQL statements 
can be displayed using the `\history` command.  The examples that follow 
refer to the following history output:

    1> \history
    (1) select * from t1
    (2) select * from t2
    (3) select * from t3
    (4) select * from t4
    (5) select * from t5

Many jsqsh command allow you to refer to SQL buffers via special syntax,
each starting with an exclamation character (`!`).  In addition, using
this syntax all by itself at the jsqsh prompt, causes the contents of
the specified buffer to be added to your current SQL buffer.  For example:

    1> !!
    1> select * from t5
    2>

causes the most recently executed statement to be placed into your 
current buffer.

The complete set of buffer references is:

* `!.`  
  Refers to the current SQL buffer that you are currently 
  typing in to (but have not yet executed).  For example, the following:

        1> select * from t6
        2> \buf-edit !.

  would pull your current buffer containg `select * from t6` into a
  text editor.
     
* `!..`  
  Refers to the most recently executed SQL buffer. For example, using
  the history shown above:

        1> !..
        1> select * from t5
        2> 

* `!!`  
   A synonym for `!..`. 
     
        1> !!
        1> select * from t5
        2>
       
* `!...`  
  Refers to the statement you executed two executions prior. Additional
  periods may be provided to continue moving back through executions 
  (e.g. `!.......` refers to the statement executed 6 executions prior). For
  example:

        1> !...
        1> select * from t4
        2>

* `!N`  
  Where *N* refers to a specific SQL buffer number as shown in `\history`.
  For example:

        1> !2
        1> select * from t2
        2>

* `!-N`  
  This syntax allows you to refer to SQL statements executed a specific
  number of executions prior. For example:

        1> !-2
        1> select * from t4
        2>
