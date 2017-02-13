## Welcome to JSqsh!
            
It looks like this is the first time that you've run jsqsh (or
you just typed '\help welcome' at the jsqsh prompt). If this is
the first time you have run jsqsh, you will find that you have
a shiney new directory called '.jsqsh' in your home directory
and that this directory contains a couple of files that you
should be aware of:
            
* drivers.xml  
  JSqsh comes pre-defined to understand how to use
  a fixed number of JDBC drivers, however this file may be
  used to teach it how to recognize other JDBC drivers. Typically
  you should not need to exit this file directly, but can
  edit it with via the jsqsh driver setup wizard, accessed
  via the `--setup` option when you run jsqsh.

* connections.xml  
  The `connections.xml` file defines connections to specific
  JDBC sources. You should not need to directly edit this
  configuration file, but should instead use the jsqsh setup
  wizard, access via the `--setup` option when you run jsqsh.

* sqshrc  
  Everything contained in this file is executed just
  as if you had typed commands at the sqsh prompt and is 
  the place where you can set variables and configure sqsh
  to your likings.
               
JSqsh is intended to be self-documenting. If you would like
to see this information again, then type '\help welcome' at 
the jsqsh prompt, or run '\help' to see a list of all help
topics that are available.
