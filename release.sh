#!/bin/sh
mvn release:prepare release:perform && git push
