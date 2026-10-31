# borba-mgrep

[![CI](https://github.com/AF2B/borba-mgrep/actions/workflows/ci.yml/badge.svg)](https://github.com/AF2B/borba-mgrep/actions/workflows/ci.yml)
[![Version](https://img.shields.io/github/v/release/AF2B/borba-mgrep?label=version)](https://github.com/AF2B/borba-mgrep/releases/latest)
[![License](https://img.shields.io/github/license/AF2B/borba-mgrep)](LICENSE)
[![Clojure](https://img.shields.io/badge/Clojure-1.12-5881D8)](https://clojure.org)
[![GraalVM native image](https://img.shields.io/badge/GraalVM-native%20image-E76F00)](https://www.graalvm.org/latest/reference-manual/native-image/)
[![Dependencies](https://img.shields.io/badge/dependencies-0-brightgreen)](deps.edn)
[![Platforms](https://img.shields.io/badge/platforms-linux%20%7C%20windows-lightgrey)](https://github.com/AF2B/borba-mgrep/releases/latest)

Grep that can also filter what it finds.

Find lines with text or a regex, then keep only the ones that pass a small
expression like `group("code") >= 500` or `line.length > 120`.

```bash
mgrep access.log --match '/status=(?<code>\d+)/' --where 'group("code") >= 500'
```

## Install

Download the archive for your system from the
[latest release](https://github.com/AF2B/borba-mgrep/releases/latest).

**Windows**

1. Extract `borba-mgrep-<version>-windows-x64.zip`.
2. Double-click `install.cmd`.
3. Open a new terminal and run `mgrep --help`.

**Linux**

```bash
tar -xzf borba-mgrep-<version>-linux-x64.tar.gz
cd borba-mgrep-<version>-linux-x64
./install.sh
```

It installs to `~/.local/bin`. If that folder is not on your `PATH`, the
script tells you what to add.

## Use

```bash
# Lines containing "error", ignoring case
mgrep app.log --match error

# Only lines longer than 120 characters
mgrep app.log --match ERROR --where 'line.length > 120'

# Several --where clauses: all of them must hold
mgrep app.log --match ERROR \
  --where 'line.contains("timeout")' \
  --where 'line.number > 100'

# Read from a pipe
tail -f app.log | mgrep --match WARN
```

### Patterns

- `error` is plain text. It ignores case by default.
- `/ERR(OR|WARN)/` is a regex, written between slashes. It respects case by
  default.
- `/id=(?<id>\w+)/` is a regex with a named group you can read in `--where`.

Force the case rule with `--case sensitive` or `--case insensitive`.

### Where expressions

| Expression                                  | Value                       |
| ------------------------------------------- | --------------------------- |
| `line.length`, `line.number`, `line.content` | Length, number and text of the line |
| `line.contains("x")`                        | `true` if the line has `x`  |
| `line.startswith("x")`, `line.endswith("x")` | `true` if it starts or ends with `x` |
| `group("name")`                             | Text captured by a named regex group |

Combine them with `>` `<` `>=` `<=` `==` `!=`, `and`, `or`, `not` and
parentheses. A captured group that looks like a number is compared as a
number, so `group("code") >= 500` works.

### Exit codes

| Code | Meaning                  |
| ---- | ------------------------ |
| `0`  | At least one line matched |
| `1`  | Nothing matched          |
| `2`  | Bad usage or an error    |

## Uninstall

**Windows:** double-click `uninstall.cmd` from the extracted folder.

**Linux:** run `./uninstall.sh` from the extracted folder.

## Develop

Needs [Babashka](https://babashka.org) and the
[Clojure CLI](https://clojure.org/guides/install_clojure). The project has no
other dependencies.

```bash
bb mgrep app.log --match ERROR   # run from source
bb test                          # tests
bb check                         # lint, format check, tests
bb build                         # native executable (needs GraalVM)
```

Source layout:

```
src/borba/mgrep/
  business/   pure logic, one folder per feature (pattern, where, pipeline)
  infra/      input and output
  cli/        arguments and exit codes
  shared/     errors and constants
```

Releases are numbered `v1`, `v2`, `v3`, and listed in the
[changelog](CHANGELOG.md). Pushing a tag such as `v1` builds and publishes the
executables.

## License

[MIT](LICENSE)

---

Developed in Clojure and distributed as a native image by GraalVM: one
executable, no JVM required.
