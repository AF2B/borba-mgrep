#!/bin/sh
# Installs mgrep for the current user.
# Set MGREP_PREFIX to install somewhere else (default: ~/.local).
set -eu

here=$(cd "$(dirname "$0")" && pwd)
prefix="${MGREP_PREFIX:-$HOME/.local}"
bin_dir="$prefix/bin"

mkdir -p "$bin_dir"
install -m 755 "$here/mgrep" "$bin_dir/mgrep"

echo "Installed mgrep to $bin_dir/mgrep"

case ":$PATH:" in
  *":$bin_dir:"*)
    echo "Run: mgrep --help"
    ;;
  *)
    echo "Add $bin_dir to your PATH, then run: mgrep --help"
    ;;
esac
