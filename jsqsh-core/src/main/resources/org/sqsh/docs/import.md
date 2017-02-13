## Synopsis

`\import [package]`

## Description

The `\import` command imports an extension package into jsqsh. An extension
package can introduce new commands and variables into jsqsh and is typically
used to extend jsqsh functionality to, say, provide functions that are 
specific to a given database platform.

## Options

### package  

`package` may be either a fully qualified directory or just the name of 
an extension package.  When just a name is provided, such as:

        1> \import foo

the package directory is expected to reside in `$JSQSH_HOME/extensions`
or `$HOME/.jsqsh/extensions`

If a package name is not provided, then a list of the available extensions
and their current state (loaded, disabled, classpath, etc.) is displayed.

## Extensions directories

Extensions should typically live in `$JSQSH_HOME/extensions` or in 
`$HOME/.jsqsh/extensions`.  The contents of this directory and details on
how to create your own extensions are included in the EXTENSIONS.md file
that is included with the jsqsh source code.
