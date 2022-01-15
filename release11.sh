#!/usr/bin/env bash

./gradlew clean build publishToSonatype -Prelease=true -Ptarget=java11 -Dorg.gradle.parallel=false -Dorg.gradle.configureondemand=false