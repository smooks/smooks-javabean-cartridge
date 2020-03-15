#!/bin/bash

mvn clean deploy -pl !perfcomp --settings .mvn/settings.xml -Dgpg.skip=false -DskipTests=true -Dmaven.javadoc.skip=true -B