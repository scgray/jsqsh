## Variable

  `csv_delimiter` - The delimiter to be used by the 'csv' rendering style

## Description

Controls the column delimiter that will be used by the 'csv' display style. The default
delimiter is a comma (,), like so:

    1> values('a', 'b')
    2> go --no-headers -m csv
    a,b

however, may be changed like so:

    1> \set csv_delimiter='|'
    1> values('a', 'b')
    2> go --no-headers -m csv
    a|b

The delimiter need not be just a single character:

    1> \set csv_delimiter=XXX
    1> values('a', 'b')
    2> go --no-headers -m csv
    aXXXb

The following special escape sequences may also be used within a delimiter

  * **\\t**
    Specifies a literal tab character
  * **\\n**
    Specifies a literal newline character
  * **\\r**
    Specifies a literal carriage return
  * **\\f**
    Specifies a literal form feed return
  * **\\ooo**
    Where `ooo` is a valid octal value, expands to the character value of the number
  * **\\xHH**
    Where `HH` is a valid hexidecimal value, expands to the character value of the number
  * **\\uHHHH**
    Where `HHHH` is a valid hexidecimal value, expands to the unicode character
    value provided
  * **\\**
    Specifies a literal '\\' character

Care should be taken to use single quotes when specifying these escape sequences
For example:

    1> \set csv_delimiter='\t'
    1> values('a', 'b')
    2> go --no-headers -m csv
    a       b

## See also

[[style]], [[csv_quote]], [[csv_quote_esc]], [[csv_null]]
