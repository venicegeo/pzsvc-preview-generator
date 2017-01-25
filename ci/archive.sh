#!/bin/bash -ex

pushd `dirname $0`/.. > /dev/null
root=$(pwd -P)
popd > /dev/null

# gather some data about the repo
source $root/ci/vars.sh

# Path to output JAR
src=$root/target/pz-svcs-prevgen*.jar

# Build Spring-boot JAR
rm -rf $HOME/.m2/*
[ -f $src ] || mvn clean package

# stage the artifact for a mvn deploy
mv $src $root/$APP.$EXT
