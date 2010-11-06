#!/bin/sh
# replace in all files the OLD_VERSION by the NEW_VERSION
# run and tested on linux.
# no need for windows support here although any community
# is always welcome to contibute such thing
if [ -z "$NEW_VERSION" ]; then
  #use the first argument as the new version
  if [ -n "$1" ]; then
    NEW_VERSION=$1
  else 
    echo "No NEW_VERSION defined."
    exit 2
  fi
fi
if [ -z "$OLD_VERSION" ]; then
  #read it in the pom.xml
  reg="<version>(.*)-SNAPSHOT<\/version>"
  line=`awk '{if ($1 ~ /'$reg'/){print $1}}' < pom.xml | head -1`
  OLD_VERSION=`echo "$line" | awk 'match($0, "<version>(.*)-SNAPSHOT</version>", a) { print a[1] }'`
fi

# reconstruct the version and buildNumber aka qualifier.
# make the assumption that the completeVersion matches a 4 seg numbers.
#if it does not then make the assumption that this buildNumber is just the forced context qualifier and use 
#the pom.xml's version for the rest of the version.
var=$(echo $NEW_VERSION | awk -F"." '{print $1,$2,$3,$4}')   
set -- $var
if [ -n "$1" -a -n "$2" -a -n "$3" -a -n "$4" ]; then
  NEW_VERSION=$1.$2.$3
  buildNumber=$4
  completeVersion="$NEW_VERSION.$buildNumber"
else
  echo "Expecting a valid OSGi version: major.minor.update.qualifier; $NEW_VERSION is incorrect"
  exit 2
fi
echo "$completeVersion"


echo "Change the version from $OLD_VERSION to $NEW_VERSION and set the forceContextQualifier to $buildNumber (default yes)"
read quiet
[ "$quiet" ] && exit 0
echo "Executing..."

#update the numbers for the release
sed -i "s/<forceContextQualifier>.*<\/forceContextQualifier>/<forceContextQualifier>$buildNumber<\/forceContextQualifier>/" pom.xml
#update the jetty-version too
sed -i "s/<jetty-version>.*<\/jetty-version>/<jetty-version>$completeVersion<\/jetty-version>/" pom.xml


#replace in the pom.xml
find . -name pom.xml -type f -exec sed -i 's/'$OLD_VERSION'/'$NEW_VERSION'/g' {} \; 

#replace in the other eclipse files where they end with a .qualifier
OLD_VERSION_QUALIFIER="$OLD_VERSION.qualifier"
NEW_VERSION_QUALIFIER="$NEW_VERSION.qualifier"
echo "$OLD_VERSION_QUALIFIER -> $NEW_VERSION_QUALIFIER"
find . -type f -exec sed -i "s/$OLD_VERSION_QUALIFIER/$NEW_VERSION_QUALIFIER/g" {} \; 

#replace in the 