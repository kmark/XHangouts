#!/bin/sh

DIR="$(cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd)"
git --git-dir="$DIR/../.git" --work-tree="$DIR/.." rev-parse --short HEAD 2> /dev/null
