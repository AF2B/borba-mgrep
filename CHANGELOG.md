# Changelog

All notable changes to this project are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Releases are numbered `v1`, `v2`, `v3`, and so on.

## Unreleased

### Added

- `--emit json` prints one JSON object per match, with the line number, the
  text and the named groups. The default stays `--emit text`.
- `--count-by NAME` prints how many matches there are for each value of a
  named regex group, most frequent first. It works with `--where` and
  `--emit json`.

## v1

### Added

- Match lines by plain text or by regex written between slashes.
- Read named groups from a regex and use them in filters.
- Filter matches with `--where` expressions over the line and the captured
  groups. Repeating the option requires every clause to hold.
- Read from a file or from standard input.
- Grep-style exit codes: `0` matched, `1` nothing matched, `2` error.
- Native executables for Linux and Windows, with install and uninstall
  scripts, published on each GitHub release.
