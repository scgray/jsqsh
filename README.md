                           Welcome to JSqsh
                      -=-=-=-=-=-=-=-=-=-=-=-=-

What is JSqsh?
==============

JSqsh (pronounced jay-skwish) is short for Java SQl Shell, a console
based tool that allows the ability to query a database with functionality
of your typical shell, such as command line editing, tab completion, variable 
expansion, redirection of output to files, or the ability to pipe the output 
of the query execution to an external program (grep, more, etc.). 

Documentation
=============

Everything you ever wanted to know about jsqsh is available here:

   https://github.com/scgray/jsqsh/wiki

I totally welcome contributions, questions, and feedback. Feel free to
contact me at scottgray1-at-gmail.com.

99.9% Pure
==========

The core of jsqsh is Java, meaning that it should compile on virtually
any platform that Java runs on.  However, there are two features of jsqsh
that are provided via an additional (optional) layer that is written in C.
These features are:

  - Command line editing (a.k.a "readline")
  - Execution of external programs that require terminal control

The second bullet means that without the C layer, programs that jsqsh spawns
that need to control the terminal (such as a text editor) or query the 
teriminal's size (such as 'more') will not function properly.

Licensing
=========

JSqsh is held under the Apache License Version 2.0. The details
of the license are available in the LICENSE file included herein.

Building and Installing
=======================

Refer to the INSTALL file for directions on building it yourself.
