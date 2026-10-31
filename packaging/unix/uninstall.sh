#!/bin/sh
# Removes the mgrep installed by install.sh.
# Set MGREP_PREFIX if you installed somewhere else (default: ~/.local).
set -eu

prefix="${MGREP_PREFIX:-$HOME/.local}"
target="$prefix/bin/mgrep"

if [ -e "$target" ]; then
  rm -f "$target"
  echo "Removed $target"
else
  echo "Nothing to remove: $target does not exist"
fi
