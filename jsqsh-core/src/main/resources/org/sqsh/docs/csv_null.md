## Variable

`csv_null` - Controls how NULL values are display by the 'csv' display style

## Description

The contents of the ${csv_null} variable are used to display null values in the 'csv'
display style.  The default value is an empty string (""), so null is displayed like so:

    1> values('a',cast(null as varchar(2)),'b')
    2> go --no-headers -m csv
    a,,b

However, you can change this like so:

    1> \set csv_null=NULL
    1> values('a',cast(null as varchar(2)),'b')
    2> go --no-headers -m csv
    a,NULL,b

## See also

[[style]], [[csv_delimiter]], [[csv_quote]], [[csv_quote_esc]]
