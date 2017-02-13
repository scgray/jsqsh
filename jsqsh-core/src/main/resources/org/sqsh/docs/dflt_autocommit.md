## Variable
  [[dflt_autocommit]] - Controls the autocommit setting for new connections

## Description

  The `${dflt_autocommit}` variables controls the SQL auto-commit setting 
  that will be set for all new connections that are created.  Auto-commit
  refers to the transaction mode that a connection uses; when auto-commit
  is true, then each statement issued is executed as if it was in its
  own transaction (that is, as if the user typed 'commit' after executing
  the statement.  When auto-commit is false, then the connection stays
  in an open transaction until you explicitly issue a commit to the database.
   
## See also

  [[autocommit]]
