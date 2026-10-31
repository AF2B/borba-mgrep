(ns borba.mgrep.cli.help
  "Text shown by --help.")

(def usage-text
  "Full help text printed by `mgrep --help`."
  "Usage: mgrep [SOURCE] --match PATTERN [options]

Print the lines of SOURCE that match PATTERN and pass every --where clause.
SOURCE is a file; omit it or use - to read standard input.

Options:
  -m, --match PATTERN  Text to find. Wrap it in slashes for a regex, which
                       may name groups: /status=(?<code>\\d+)/
  -w, --where EXPR     Keep a match only if EXPR is true. Repeat it to add
                       clauses; all of them must hold.
      --case MODE      sensitive or insensitive. Text ignores case by default,
                       regexes respect it.
      --emit FORMAT    text (default) prints the lines. json prints one object
                       per match with its line number and named groups.
  -h, --help           Show this help.
      --version        Show the version.

Where expressions:
  line.length  line.number  line.content
  line.contains(\"text\")  line.startswith(\"text\")  line.endswith(\"text\")
  group(\"name\")           a named group captured by --match
  > < >= <= == !=  and  or  not  ( )

Examples:
  mgrep app.log --match ERROR
  mgrep app.log --match ERROR --where 'line.length > 120'
  mgrep access.log --match '/status=(?<code>\\d+)/' \\
        --where 'group(\"code\") >= 500'
  tail -f app.log | mgrep --match WARN

Exit codes: 0 a line matched, 1 nothing matched, 2 error.
")
