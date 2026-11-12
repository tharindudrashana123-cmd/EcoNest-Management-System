#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
mkdir -p build
javac -encoding UTF-8 -d build src/EcoNestApp.java
java -cp build EcoNestApp
