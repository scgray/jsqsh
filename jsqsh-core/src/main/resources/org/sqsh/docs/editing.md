## JSqsh line editing

  If you were using JSqsh prior to version 3.0, you should definitely
  read this section!

  JSqsh uses a library called [JLine3](https://github.com/jline/jline3) to
  provide the ability to do line editing.  This library, provides the basic
  functionality of being able to move around with the cursor keys, insert,
  edit, delete, and other similar options.

  As of JSqsh 3.0 the version of JLine was changed from JLine2 to JLine3.
  JLine3 includes a rather significant enhancement allowing you to edit more
  than just the current line but also edit up and down lines through the entire
  SQL statement!

## Recognizing a statement

  Every time you hit the ENTER (or RETURN) key, jsqsh looks at the current
  line of text your cursor is sitting on and tries to determine what to do
  with it.  The logic goes like this (in the order shown):

  * If the line begins with a jsqsh command, then current statement is
    considered to be "finished" and the command is then executed. The obvious
    example is the `go` command:

        1> select *
        2> from foo
        3> where x > 10
        4> go

        +----+----+
        |  x |  y |
        +----+----+
        | 11 |  a |
        +----+----+
        ...
        1> _

     Some statements, such as `go` will take the statement, save it away
     in the history list (see [\history]) and then clear out the current
     statement (as show above...the _ represents the position of the prompt).

     However, some statements do not do anything with the current SQL
     buffer, so jsqsh will re-display the statement being typed so that
     you can continue to edit it, for example:

        1> select *
        2> from foo
        3> where x > 10
        4> \echo hello
        hello
        1> select *
        2> from foo
        3> where x > 10
        4> _

     (again, the _ is to represent where the cursor is).

     This can sometimes appear kind of strange, because you could also move
     around and insert a command into the middle of a statement, for example
     let's say you had typed:

        1> select *
        2> from foo _

     If you moved up one line, and inserted a command, such as:

        1> select *
        2> \echo hello _
        3> from foo

     when you hit ENTER, you will see the following:

        hello
        1> select *
        2> from foo
        3> _

  * If the line begins with a buffer recall command (e.g. !!, see [buffers]),
    then the recall command is executed in the position that it was inserted,
    for example:

        1> begin transaction
        2> !! _
        3> commit transaction

     when you hit ENTER with the cursor in the position shown, you will see

        1> begin transaction
        2> delete foo
        3> where x > 10
        4> commit transaction
        5> _

     that is, the "delete" statement was recalled from history and inserted
     at the point shown.

  * If the current line is terminated (which is usually using a semicolon (;),
    see [terminator]) and the cursor is sitting on or after the terminator, then
    the statement is executed as if you had typed `go`:

        1> select * from foo; _

    hitting ENTER at this point will give you:

        1> select * from foo; _

        +----+----+
        |  x |  y |
        +----+----+
        | 11 |  a |
        +----+----+
        ...
        1> _

    However, if the cursor was sitting before the semicolon, then the assumption
    is that you want to break the current line, like so:

        1> select *_ from foo;

    hitting ENTER at this point would result in:

        1> select *
        2> _ from foo;

## Running a statement with CTRL-G

  With JSqsh 3.0 you may now also execute the current buffer by hitting CTRL-G
  which forces the contents of the buffer to be executed as if you had used
  the `go` command, for example:

        1> select * from foo_

  hitting CTRL-G at this point would result in:

        1> select * from foo

        +----+----+
        |  x |  y |
        +----+----+
        | 11 |  a |
        +----+----+
        ...
