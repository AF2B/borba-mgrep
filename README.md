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

## Examples

The general form is:

```
mgrep <file> --match <pattern> [--where <expression>]... [--case sensitive|insensitive] [--emit text|json]
```

`--match` is required. Everything in square brackets is optional, and
`--where` can be repeated. Leave out `<file>` to read from a pipe.

Create a small log to try the commands below:

```bash
cat > app.log <<'EOF'
2026-10-31 10:00:01 INFO  server started port=8080
2026-10-31 10:00:05 WARN  slow query took=1200ms
2026-10-31 10:00:09 ERROR status=503 upstream timeout
2026-10-31 10:00:12 INFO  GET /health status=200
2026-10-31 10:00:15 ERROR status=404 route not found
2026-10-31 10:00:21 ERROR status=500 db connection lost
EOF
```

### 1. Find a word

```bash
mgrep app.log --match error
```

```
2026-10-31 10:00:09 ERROR status=503 upstream timeout
2026-10-31 10:00:15 ERROR status=404 route not found
2026-10-31 10:00:21 ERROR status=500 db connection lost
```

Plain text ignores case, so `error` found `ERROR`. Add `--case sensitive`
to make it exact.

### 2. Find a regex

Write it between slashes:

```bash
mgrep app.log --match '/status=5\d\d/'
```

```
2026-10-31 10:00:09 ERROR status=503 upstream timeout
2026-10-31 10:00:21 ERROR status=500 db connection lost
```

### 3. Keep only some matches with `--where`

`--where` runs on each line that matched.

```bash
# Only the lines after line 4
mgrep app.log --match ERROR --where 'line.number > 4'

# Only the lines that end with "lost"
mgrep app.log --match ERROR --where 'line.endswith("lost")'

# Either word
mgrep app.log --match ERROR \
  --where 'line.contains("timeout") or line.contains("lost")'

# Everything except INFO (an empty --match matches every line)
mgrep app.log --match '' --where 'not line.contains("INFO")'
```

### 4. Use a number from the line

Name a group in the regex, then read it with `group("name")`:

```bash
mgrep app.log --match '/status=(?<code>\d+)/' --where 'group("code") >= 500'
```

```
2026-10-31 10:00:09 ERROR status=503 upstream timeout
2026-10-31 10:00:21 ERROR status=500 db connection lost
```

`code` is text, but it looks like a number, so `>= 500` compares it as one.

### 5. Combine several filters

Repeat `--where`. Every clause must hold:

```bash
mgrep app.log --match ERROR \
  --where 'line.number > 3' \
  --where 'line.contains("not found")'
```

```
2026-10-31 10:00:15 ERROR status=404 route not found
```

### 6. Read from a pipe

Leave out the file name:

```bash
cat app.log | mgrep --match WARN
tail -f app.log | mgrep --match ERROR
```

### 7. Get JSON for other tools

Add `--emit json` to print one JSON object per match, with the line number,
the text and the groups you named:

```bash
mgrep app.log --match '/status=(?<code>\d+)/' --where 'group("code") >= 500' --emit json
```

```
{"line":3,"content":"2026-10-31 10:00:09 ERROR status=503 upstream timeout","groups":{"code":"503"}}
{"line":6,"content":"2026-10-31 10:00:21 ERROR status=500 db connection lost","groups":{"code":"500"}}
```

Each line is a complete JSON document, so tools like `jq` read it directly.

### 8. Use it in scripts

The exit code says what happened: `0` matched, `1` nothing matched, `2` error.

```bash
# Count the errors
mgrep app.log --match ERROR | wc -l

# Act when something is found
mgrep app.log --match FATAL || echo "no fatal errors"
```

### On Windows

`cmd` and PowerShell treat quotes differently from Linux shells. Wrap the
whole expression in double quotes and use single quotes inside it:

```powershell
mgrep app.log --match "/status=(?<code>\d+)/" --where "group('code') >= 500"
mgrep app.log --match ERROR --where "line.contains('timeout')"
```

### When something goes wrong

Errors go to stderr and the exit code is `2`:

```
$ mgrep app.log
mgrep: missing --match PATTERN
Try 'mgrep --help' for usage.

$ mgrep app.log --match ERROR --where 'line.size > 1'
mgrep: unknown attribute line.size at position 5

$ mgrep missing.log --match ERROR
mgrep: no such file: missing.log
```

Run `mgrep --help` to see every option.

## Reference

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

Every command lives in the `Makefile`. Run `make` to list them.

Needs [Babashka](https://babashka.org), the
[Clojure CLI](https://clojure.org/guides/install_clojure) and `make`. The
project has no other dependencies.

```bash
make                                    # list every command
make run ARGS="app.log --match ERROR"   # run from source
make test                               # tests
make check                              # lint, format check, tests
make build                              # native executable (needs GraalVM)
make release                            # tag and publish the declared version
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
[changelog](CHANGELOG.md). `make release` pushes the tag, and the tag builds
and publishes the executables.

## License

[MIT](LICENSE)

---

Developed in Clojure and distributed as a native image by GraalVM: one
executable, no JVM required.
