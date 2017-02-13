## Variable

  `scale` - Controls digits displayed after decimal for floating point values

## Description

  When display floating point values, `${scale}` determines how many of the
  total `${precision}` digits are used to represent values following the
  decimal place.  For example:
   
    Actual Value  Precision Scale      Displayed Value
    ------------  --------- ---------- ---------------
       1.2345678          5          2            1.23
       1.2345678          5          3           1.234
       1.2345678          5          4          1.2345
           
  This property only applies to types of real, float, double and Oracle 
  NUMERIC (with no precision or scale specified).

## See also

  [[precision]]
