## Synopsis

`\help [--raw] [[topics|vars|commands] | item]`

## Description

Displays help for a jsqsh command. If no arguments are provided,
`\help` provides a list of available categories of help: topics, vars
(variables), or commands. Running \help with one of those category
names lists a set of help items available within the category.
  
## Options

### --format *format* (-f *format*)

Displays help text in one of the following display styles:

* `pretty` - This is the default mode. Help text is displayed nicely
  formatted to the width of your terminal and with text decorations
  (bolding and underline).
* `raw` - Help text is displayed in a nicely formatted style, but  
  with all text decorations (bold and underline) disabled. This is
  useful if you are using jsqsh from a terminal that does not support
  such decorations, or if you wish to redirect the help text to a
  text file.
* `markdown` - Help text is displayed in markdown format
  (see https://daringfireball.net/projects/markdown/).

### topics

Displays a list of general help topics about jsqsh

### vars

Displays a list of all variables that control jsqsh's behavior

### commands

Displays a list of all jsqsh commands

### *item*

Given the name of a topic, variable, or command displays detailed 
help for it.
  
## Command line option

All jsqsh commands take a special command line option of `--help` or `-h`
which will cause help to be displayed on the available command line
options available for the command.

## See also

[[arguments]]
