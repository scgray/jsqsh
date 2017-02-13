## JSqsh line editing

Jsqsh can utilize several different line editing libraries that allow
you to move around and edit the current line you are typing at, as well
as to scroll backwards and forwards through previous lines of text
that you have typed in.  Note that this is different from recalling
and editing entire statements: for that, please see the `\help` for 
[[buffers]], [[\history|history]], and [[\buf-edit|\buf_edit|buf-edit|\buf_edit]].

## Choosing a line editor

JSqsh uses JLine as the default line editor (see below).  However,
it supports a number of editors, You can control which editor is used 
via either the `--readline` command line option when starting jsqsh, 
or the `JSQSH_READLINE` environment variable, so to one of the values 
below:

Value     | Description
----------|-------------------------------------------
jline     | JLine (built-in)
readline  | GNU Readline (via java-readline, see below)
editline  | BSD Editline (via java-readline, see below)
getline   | Getline (via java-readline, see below)
none      | None (no line editor)

## Jline line editing

As of JSqsh 1.5, the default line editor is JLine:

https://github.com/jline/jline2

This library provides full line editing, history recall, searching,
emacs and vi keymaps, configurable keymaps and a bunch of other 
functionality.  The JLine library is not 100% java and relies upon
some native code.  As a result, it is only supported on Windows (32
and 64 bit), Linux (32 and 64 bit) and MacOS.

## GNU readline support

GNU readline used to be the default editor in JSqsh, however as of
JSqsh 1.5, JLine is now the default and readline is not included
with the JSqsh distribution. This was due to both the JSqsh license 
change to the Apache license, as well as to the fact that the 
Java-Readline library appears to be abandoned. 

While Jsqsh retains its support for Java-Readline, the library
is not included with the jsqsh distribution.  If you wish to utilize
the library, most Linux distributions can easily install
Java-Readline using the software package manager (typically under
a package named "libreadline-java").

## Java-readline detection
   
The JSqsh startup script will automaticallyl detect and utilize
java-readline if it is installed on your system on a debian based
linux distribution (e.g. Debian, Ubuntu, Mint, etc.).  If these are found, 
then readline support will be enabled.
