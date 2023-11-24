#!/bin/bash
set -e

function fatal() { echo "🛑 $1"; exit 1; }

[[ $(git symbolic-ref --short -q HEAD) =~ (^master) ]] || fatal "not in master branch";
[[ -z "$(git status --porcelain)" ]] || fatal "not clean git workspace";

version=$1
if [[ -z ${version} ]]; then
  lastVersion=$(git fetch --tags && git describe --tags "$(git rev-list --tags --max-count=1)")
  read -rp "🤔 next version (last was ${lastVersion})? " version
fi
[[ ${version} =~ ^[0-9]+[.][0-9]+ ]] || fatal "version should match N.NNN but was ${version}";
if git rev-parse "${version}" >/dev/null 2>&1; then fatal "tag ${version} already exists"; fi

# install and compile only test module to verify that shaded dependencies cover ALL required dependencies
# note that if you just run "mvn verify", reactor will include the processor dependencies! we don't want it!
mvn clean install && mvn -pl test clean test

mvn release:prepare release:perform -DreleaseVersion=$version -Dtag=v$version