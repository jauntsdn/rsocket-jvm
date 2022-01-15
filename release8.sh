#!/usr/bin/env bash

./gradlew clean build publishToSonatype -Prelease=true -Ptarget=java8 -Dorg.gradle.parallel=false -Dorg.gradle.configureondemand=false