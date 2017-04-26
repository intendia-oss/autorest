#!/bin/sh
if [ -z "$1" ];
then echo "usage: release.sh <releaseVersion>"
else mvn release:prepare release:perform -DreleaseVersion=$1 -Dtag=v$1
fi
