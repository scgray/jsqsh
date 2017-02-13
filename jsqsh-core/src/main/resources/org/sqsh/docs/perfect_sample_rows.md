## Variable

  `perfect_sample_rows` - Controls the display of query footer information
   
## Description

  The `${perfect_sample_rows}` determines the number of rows that are 
  sampled (held in memory) by perfect result renderers (such as used 
  by the "perfect" display style) before results are displayed.  The
  larger this number, the better the results will be rendered, however
  the more memory will be consumed as all the rows must be held in memory
  before display.
   
  A value less than 1 indicates that all rows will be sampled prior to
  display.  The default value is 500.
   
## See also

  [[style]]
