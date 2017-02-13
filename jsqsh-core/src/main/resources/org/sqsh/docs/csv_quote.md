## Variable

  `csv_quote` - The quoting character to be used during 'csv' display style

## Description

When output is produced by the 'csv' display style (see [[style]]), any column that meets
the following criteria will be surrounded by the quotes specified by this variable:

  * The value contains leading or trailing white-space
  * The value contains the delimiter character
  * The value contains a newline character

The default quote is a double-quote, like so:

    1> values('hi,there', 'a', ',', 'b')
    2> go --no-headers -m csv
    "hi,there",a,",",b

The ${csv_quote} variable may be used to change which character is used for quoting, such as:

    1> \set csv_quote=\'
    1> values('hi,there', 'a', ',', 'b')
    2> go --no-headers -m csv
    'hi,there',a,',',b

Note that the quote need not be a single character:

    1> \set csv_quote=OH_NO
    1> values('hi,there', 'a', ',', 'b')
    2> go --no-headers -m csv
    OH_NOhi,thereOH_NO,a,OH_NO,OH_NO,b

For columns that contain the quoting character, the default behavior is to double the
quoting character like so:

    1> \set csv_quote=\"
    1> values('hi "Scott"', 'a')
    2> go --no-headers -m csv
    "hi ""Scott""",a

However this can be changed via the $[[csv_quote_esc]] variable.

## See also

[[style]], [[csv_delimiter]], [[csv_quote_esc]], [[csv_null]]
