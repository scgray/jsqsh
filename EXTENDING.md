# Extending JSqsh

JSqsh version 2.3 introduced a mechanism by which jsqsh can be dynamically
extended with new commands and variables.  

Prior to this feature being introduced, such changes had to be made directly 
to the base jsqsh source code -- while this wasn't a horrible thing, the 
biggest problem was that if you added new commands to jsqsh that needed 
external jars that didn't exist on your system, then jsqsh couldn't even 
start! Now, jsqsh can attempt to load new sets of commands at runtime and
if the dependent jars are not available you get a pleasant error message
and jsqsh is still otherwise usable.

This document describes how to develop your own jsqsh extensions.

## The `\import` command

The `\import` command is pretty well documented in the built-in documentation
but at its heart it takes a path to an extension to import (if you don't provide
the full path it assumes it is located at `$JSQSH/extensions/name`).

An extension is just a directory, and in the directory may be found a number of
things:

  * Any number of jars that implement the extension
  * A 'jsqsh-extension.conf'.  This is a properties file that defines some
    attributes of the extension.  The supported contents of this file is
    defined in the javadocs for org.sqsh.ExtensionManager.  It controls 
    things like additional classpath entries needed for the extension,
    when the extension gets loaded, etc.
  * A `Commands.xml` file.  This file defines a jsqsh command. The definition
    includes the name of the command, the class that implements the command
    and the documentation for the command.  While I wish I had documentation
    for this file, you can find lots and lots of examples of command 
    definitions in the jsqsh source code under 
    `src/resources/org/sqsh/commands/Commands.xml`.
  * A `Variables.xml` file.  This file defines new configuration variables
    for jsqsh, which includes the name of the variable, a class that 
    implements the variable (which can do nifty things like checking 
    the validity of a setting) and a default value.
    extension. This script may be called 'classpath' or 'classpath.sh' on
    UNIX, or 'classpath.bat' or 'classpath.exe' on Windows.   The script
    will be executed prior to loading the command jar files and can be
    used to pull in jars and directories from other locations that
    may help to implement the command.

## Example extension

Jsqsh comes with a sample extension called 'test', located in the
source code under 'jsqsh-exttest', which provides a pretty good 
template on how to build and package an extension.  

You can use the extension at runtime in jsqsh with:

    1> \import test
    1> \hello
    Hello World!!
    1> \echo $foo
    0

