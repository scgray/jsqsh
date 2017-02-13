## Synopsis

`\read [--prompt prompt] [--silent] [--if-unset] var [var ...]`

## Description

The `\read` command reads one line of input from the console and assigns
the first word of that input to `var`, the second word to the second `var`,
and so on, with all remaining input provided assigned to the final `var`.
If there are fewer words read from the input than `var`s provided, then the
remaining vars are assigned empty values.

For example:

    1> \read --prompt="Type: " x y z
    Type: hello how are you?
    1> \echo $x
    hello
    1> \echo $y
    how
    1> \echo $z
    are you?

## Options

### --if-unset (-u)

Indicates that the user should only be prompted if one or more of the
variable names provided does not yet have a value.  For example:

    1> \read --prompt="Type something: " x
    Type something: Hi!
    1> \read --if-unset --prompt="Type something: " x
    1>

In the second instance, the read does not occur because `$x` already
has a value.

### --prompt=*prompt* (-p *prompt*)

Provides text to be used when prompting the user for input, such as:

    1> \read --prompt="--> " x
    -->

### --silent (-s)

Indicates that the text entered by the user is sensitive and should not
be echoed back to the screen, instead each character will be presented as
an asterisk (*).  Yes, this isn't "silent", but the parameter name is
"silent" because that is what most shells indicate. For example:

   1> \read --prompt="--> " --silent x
   --> ***
