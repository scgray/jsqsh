## Variable

   `exec_mode` - Controls the manner in which SQL execution is performed.

## Description

   By default JSqsh sends SQL to the remote database server in "immediate"
   mode (for those of you savvy with Java, it is `Connection.execute(...)`),
   which means that the server just takes the SQL, parses, and runs it. 
   For certain database platforms, however, certain types of statements cannot
   be run in immediate mode.  For example DB2 z/OS cannot execute a procedure
   call (`CALL procname`) in immediate mode--the SQL must first be prepared
   (It's silly, and I don't know why).

   This variable switches the mode in which JSqsh sends SQL to the remote
   server.  It may be set to either `immediate` or `prepare`. 

   It is rare that you should ever need to change this variable. Most
   driver definitions in jsqsh will automatically set this to the correct
   value when you connect to the server.
   
## See also

   None.
