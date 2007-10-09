Summary: Command line shell for working with SQL databases
Name: jsqsh
Version: @VERSION@
Release: 1
Group: Applications/Databases
License: GPL
Source0: jsqsh-@VERSION@-src.tar.gz
BuildRoot: %{_builddir}/jsqsh-@VERSION@/build/usr
Requires: libreadline-java >= 0.8.0

%description
JSqsh (pronounced J-skwish) is short for Java SQshelL
(pronounced s-q-shell), JSqsh is much more than a nice
prompt, it is intended to provide much of the
functionality provided by a good shell, such as variables,
redirection, pipes, back-grounding, job control, history,
command completion, and dynamic configuration. Also, as
a by-product of the design, it is remarkably easy to
extend and add functionality.

%prep
%setup -q

%build
echo Move along, there is nothing to see here

%install
ant dist-rpm-install

%clean
