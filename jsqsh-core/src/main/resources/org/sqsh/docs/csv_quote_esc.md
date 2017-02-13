## Variable

  `csv_quote_esc` - Controls how quotes are escaped by 'csv' display style

## Description

When a field is output in the 'csv' display style and contains a quoting character or
string (see $[[csv_quote]]), the value of this variable is used to "escape" the quote.
When this variable is empty (the default), the default behavior is to double whatever
the current quote is, for example:

    1> values('Sally said, "Hello Scott!"', 10)
    2> go --no-headers -m csv
    "Sally said, ""Hello Scott!"",10

However, you may override this behavior with:

    1> \set csv_quote_esc=\\
    2> go --no-headers -m csv
    "Sally said, \"Hello Scott!\",10

## See also

[[style]], [[csv_delimiter]], [[csv_quote]], [[csv_null]]
