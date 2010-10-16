#!/bin/bash

# set path to eclipse folder. If local folder, use '.'; otherwise, use /path/to/eclipse/
#resolve the symbolic link if it is one. does not work on mac
script=$0;
#resolve the symbolic link if it is one. does not work on mac
[ -h "$script" ] && script=`readlink -f $script`
eclipsehome=`dirname $script`;
[ -h "$eclipsehome" ] && eclipsehome=`readlink -f $eclipsehome`

# get path to equinox jar inside $eclipsehome folder
ini=$(find $eclipsehome -maxdepth 1 -name "*.ini" | sort | tail -1);
if [ -z "$JAVA_OPTS" ]; then
    JAVA_OPTS="-XX:MaxPermSize=384m -Xms96m -Xmx784m"
fi

if [ -f $ini ]
  args=`cat $ini | tr '\n' ' ' | awk -F'-startup ' '{print $2}'`
else
  args=$(find $eclipsehome -name "org.eclipse.equinox.launcher_*.jar" | sort | tail -1);
fi
buildfile=
consoleLog=""
while getopts hvf: OPTION
do
    case ${OPTION} in
        f) buildfile="$OPTARG";;
        v) consoleLog="-consoleLog -verbose ";;
        h) printf "Usage: %s: -f buildfile.xml [-v (verbose aka consoleLog)] args\n" $(basename $0) >&2
			exit 2
			;;
    esac
done


if [ ! -f "$args" ]; then
  #was returned as path relative to eclipsehome and the working directory is not eclipse home
  args="$eclipsehome/$args"
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


echo "java $JAVA_OPTS -jar $args$configurationArg$installArg -buildfile $buildfile $consoleLog $*"
java $JAVA_OPTS -jar $args$configurationArg$installArg -buildfile $buildfile $consoleLog $*
