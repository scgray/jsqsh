# Welcome to JSqsh

JSqsh (pronounced jay-skwish) is short for Java SQl Shell, a console
based tool that allows the ability to query a database with functionality
of your typical shell, such as command line editing, tab completion, variable 
expansion, redirection of output to files, or the ability to pipe the output 
of the query execution to an external program (grep, more, etc.). 

![Awesome Screenshot](https://cloud.githubusercontent.com/assets/1461324/4958665/1ba4fc86-66b1-11e4-95b5-9a53cd7f47c8.jpg)

JSqsh has extensive built-in documentation, however for those that prefer
point-and-click learning, you can get an introduction to jsqsh using
the jsqsh wiki:

* [Wiki Home](https://github.com/scgray/jsqsh/wiki/Home)
* [What's New?](https://github.com/scgray/jsqsh/wiki/What%27s-New%3F)
* [Features](https://github.com/scgray/jsqsh/wiki/Features)
* [Installing](https://github.com/scgray/jsqsh/wiki/Installing)
* [Getting Started](https://github.com/scgray/jsqsh/wiki/Getting-Started)
* [User's Guide](https://github.com/scgray/jsqsh/wiki/User%27s-Guide)
* [Tips and Trick](https://github.com/scgray/jsqsh/wiki/Tips-and-Tricks)
* [Change History](https://github.com/scgray/jsqsh/wiki/Change-History)

I totally welcome contributions, questions, and feedback. Feel free to
contact me at scottgray1-at-gmail.com.

## Licensing

JSqsh is held under the Apache License Version 2.0. The details
of the license are available in the LICENSE file included herein.

## 99.9% Pure

The core of jsqsh is Java, meaning that it should compile on virtually
any platform that Java runs on.  However, there are two features of jsqsh
that are provided via an additional (optional) layer that is written in C.
These features are:

  - Command line editing (a.k.a "readline")
  - Execution of external programs that require terminal control

The second bullet means that without the C layer, programs that jsqsh spawns
that need to control the terminal (such as a text editor) or query the 
terminal's size (such as 'more') will not function properly.
