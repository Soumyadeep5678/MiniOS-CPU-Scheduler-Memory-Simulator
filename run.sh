#!/usr/bin/env sh
# Build and run MiniOS with only a JDK (17+); no Maven needed.
#   ./run.sh                 interactive menu
#   ./run.sh bench           any command from: ./run.sh help
set -e
cd "$(dirname "$0")"
rm -rf build/classes
mkdir -p build/classes
javac --release 17 -d build/classes $(find src/main/java -name '*.java')
exec java -cp build/classes com.minios.Main "$@"
