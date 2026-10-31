(ns borba.mgrep.shared.constants
  "Values shared across layers, named so no literal hides its meaning.")

(def program-name
  "Name used in usage text and error prefixes."
  "mgrep")

(def version
  "Release identifier printed by --version. Must match the git tag."
  "v1")

(def stdin-marker
  "Source argument that selects standard input."
  "-")

(def first-line-number
  "Number of the first line of any source."
  1)

(def exit-matched
  "Exit code when at least one line matched."
  0)

(def exit-no-match
  "Exit code when the run succeeded and nothing matched."
  1)

(def exit-error
  "Exit code when usage, input or filter evaluation failed."
  2)
