#!/usr/bin/env bash
# Copies the mod jar of every target into jars/ at the repository root (a local folder, not in git).
# Run it after building: "./gradlew build" in each target folder, or pass --build to build them all
# first.
#
# Author: vyrriox
set -euo pipefail

ROOT=$(git rev-parse --show-toplevel)
cd "$ROOT"
mkdir -p jars

for dir in */; do
    target=${dir%/}
    if [[ ! "$target" =~ ^(neoforge|forge|fabric)-.+$ ]] || [ ! -f "$target/build.gradle" ]; then
        continue
    fi
    if [ "${1:-}" = "--build" ]; then
        echo "== building $target"
        (cd "$target" && ./gradlew build --console=plain -q)
    fi
    # The mod jar only: not the sources, dev or javadoc jars.
    found=0
    for jar in "$target"/build/libs/lauramod-*.jar; do
        [ -e "$jar" ] || continue
        case "$jar" in
            *-sources.jar | *-dev.jar | *-javadoc.jar | *-dev-shadow.jar) continue ;;
        esac
        cp "$jar" jars/
        echo "$target: $(basename "$jar")"
        found=1
    done
    if [ "$found" = 0 ]; then
        echo "$target: no jar, build it first" >&2
    fi
done
