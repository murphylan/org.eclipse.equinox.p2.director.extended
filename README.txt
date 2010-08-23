Extends p2 director to add:
Support for parsing arguments in properties files.
--------------------------------------------------
-props uri or file path
will load the arguments in the corresponding properties file.
System properties are evaluated using the synthax ${sysprop} or ${sysprop,defaultvalue}


Support for the flag -addSources
--------------------------------
It will install all the source bundles that can be found for the installed product.
Reference thread: http://dev.eclipse.org/mhonarc/lists/p2-dev/msg03251.html

Examples:
---------
From eclipse PDE, execute one of the 2 launch configurations.