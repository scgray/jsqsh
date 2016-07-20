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

## Extensions directories

JSqsh extensions can live in `$JSQSH_HOME/extensions` or `$HOME/.jsqsh/extensions`.
Each sub-directory underneath of there is expected to represent a jsqsh extension.
A typical extension directory should look like:

<pre>
   $JSQSH_HOME/extensions/
      myextension/
         jsqsh-extension.conf
         Commands.xml
         Variables.xml
         MyExtension.jar
         SomeOtherJar.jar
</pre>

Where these files are:

  * A `jsqsh-extension.conf`.  This is a properties file that defines some
    attributes of the extension.  The supported contents of this file is
    defined in the javadocs for `org.sqsh.ExtensionManager`.  It controls 
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
  * Any number of jars that implement the extension

## Loading extensions

An extension can be loaded in the following ways:

  * A user may explicitly load the extension with the `\import` command
  * The extension may be set to automatically load when jsqsh starts
    via the `load.on.start` property in `jsqsh-extensions.conf`
  * An extension may be automatically loaded when a particular jdbc 
    driver becomes available via the 'load.on.driver' property in the
    jsqsh-extensions.conf file. 

## Extension classpaths

As noted above, an extension may include all of the jars it needs to
implement itself, however extensions have other ways to find
dependencies:

  * The `jsqsh-extension.conf` file may specify the name of a script
    to run that will return a classpath needed for the extension
    (via the `classpath.script.unix` and `classpath.script.win`
    properties). 
  * When an extension is automatically loaded when a particular jdbc
    driver becomes available (via the `load.on.drivers` property),
    the extension automatically inherits the classpath of the driver
    that caused it to be loaded.

## More complex extension behavior

Extensions wanting to do more than just install commands and variables
may provide an `ExtensionConfigurator` class via the `load.config.class`
property in `jsqsh-extension.conf`.  This class will be instantiated
and invoked, passing it the main jsqsh class (SqshContext) and from there
may do pretty much anything it needs.

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

