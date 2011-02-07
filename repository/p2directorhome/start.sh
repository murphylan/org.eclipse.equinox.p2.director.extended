#!/bin/sh -e
#
# set path to eclipse folder. If local folder, use '.'; otherwise, use /path/to/eclipse/
#resolve the symbolic link if it is one. does not work on mac
script=$0;
#resolve the symbolic link if it is one. does not work on mac
[ -h "$script" ] && script=`readlink -f $script`
eclipsehome=`dirname $script`;
[ -h "$eclipsehome" ] && eclipsehome=`readlink -f $eclipsehome`

#make sure we don't get into weird profile lock issues.
[ -d /tmp/p2 ] && rm -rf /tmp/p2

iniLookupFolder=$eclipsehome
# get path to equinox jar inside $eclipsehome folder
#echo "looking in $eclip#sehome"
ini=$(find $eclipsehome -mindepth 1 -maxdepth 1 -name "*.ini" | sort | tail -1);
if [ ! -f "$ini" ]; then
#maybe a mac
appFolder=$(find $eclipsehome -mindepth 1 -maxdepth 1 -type d -name "*.app" | sort | tail -1);
  if [ ! -d "$appFolder" ]; then
    echo "Unable to find equinox inside $eclipsehome"
    #return 1;
  fi
  iniLookupFolder="$appFolder/Contents/MacOS"
  if [ -d "$iniLookupFolder" ]; then
    ini=$(find $iniLookupFolder -mindepth 1 -maxdepth 1 -type f -name "*.ini" | sort | tail -1);
  fi
fi
if [ -f "$ini" ]; then
  args=`cat $ini | tr '\n' ' ' | awk -F'-startup ' '{print $2}'`
else
  #this only works for a standalone (aka "roaming") install
  args=$(find $eclipsehome -name "org.eclipse.equinox.launcher_*.jar" | sort | tail -1);
fi
if [ ! -f "$args" ]; then
  #was returned as path relative to iniLookupFolder
  args="$iniLookupFolder/$args"
fi

if [ -z "$JAVA_OPTS" ]; then
    JAVA_OPTS="-XX:MaxPermSize=384m -Xms96m -Xmx784m"
fi

#use -install unless it was already specified in the ini file:
installArg=
if echo $args | grep -Eq ' -install'
then
    #echo "-install already defined in the ini file"
    installArg=""
else
    installArg=" -install $eclipsehome"
fi

#use -configuration unless it was already specified in the ini file:
configurationArg=
if echo "$args $*" | grep -Eq ' -configuration'
then
    #echo "-install already defined in the ini file"
    configurationArg=""
else
    tmp_config_area=`mktemp -d /tmp/directorHeadlessConfigArea.XXXXXX`
    configurationArg=" -configuration $tmp_config_area"
fi

#todo: read the ini file some more:
#read the line immediately after the line that maches the regexp: sed -n '/regexp/{n;p;}'
#see also http://codesnippets.joyent.com/posts/show/2043

#the director application
#application="-application org.eclipse.equinox.p2.director.extended"

echo "java $JAVA_OPTS -jar $args$installArg$configurationArg $application $*"
java $JAVA_OPTS -jar $args$installArg$configurationArg $application $*
