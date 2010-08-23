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
The second one executes this properties file which installs RTOSGiStarterKit and the source bundles:

#example properties to install the rtosgistarterkit with sources
#-repository=http://download.eclipse.org/releases/helios/
-repository=${eclipse.mirrorurl,http://d2u376ub0heus3.cloudfront.net}/jetty/7.1.4.v20100610/repository
-installIU=org.eclipse.rt.osgistarterkit
-destination=${user.home}/.p2/tmp/osgistartkit
-profile=EclipseRTOSGiStarterKit
-bundlepool=${user.home}/.p2/test2
-addSources