## Variable

  `maxrows_method` - Controls the mechanism used to limit result set rows

## Description

  The `${maxrows_method}` variable is used to control the mechanism that is
  used to limit result rows as specified by the `${maxrows}` variable. This
  variable may be one of the following:
   
  * `discard` (default)  
    This causes jsqsh to silently discard all rows beyond 
   `${maxrows}`. This is the safest and recommended mechanism to utilize 
   but can cause delays as additional network traffic as the rows are processed.
     
  * `cancel`  
    This causes jsqsh to issue a cancel to stop the processing of the query 
    when `${maxrows}` is reached. This can cause side effects because it cancels 
    the entire query, not just the current result set. For example, if you 
    were calling a stored procedure or a SQL batch that contained multiple 
    statements or queries, the cancel would stop the processing at the first 
    query that returned more than `${maxrows}`.
    
  * `driver`  
    This causes jsqsh to ask the JDBC driver to limit row results to 
    `${maxrows}` (via the drivers `Statement.setMaxRows()` method). This method 
    has been shown to have side effects in some drivers of not just limiting 
    the result rows but also limiting UPDATEs and DELETEs to `${maxrows}`.
               
## See also

  [[maxrows]]
