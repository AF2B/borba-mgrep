(ns borba.mgrep.business.pattern.core
  "Turns the `--match` argument into a line matcher.

  A pattern wrapped in slashes (`/.../`) is a regular expression; anything
  else is a plain substring."
  (:require
   [borba.mgrep.business.pattern.literal :as literal]
   [borba.mgrep.business.pattern.regex :as regex]
   [clojure.string :as str]))

(def ^:private regex-delimiter "/")

(def ^:private minimum-regex-length
  "Shortest pattern that can hold both delimiters."
  2)

(def ^:private literal-case-sensitive-by-default? false)

(def ^:private regex-case-sensitive-by-default? true)

(defn- regex-pattern?
  [pattern]
  (and (>= (count pattern) minimum-regex-length)
       (str/starts-with? pattern regex-delimiter)
       (str/ends-with? pattern regex-delimiter)))

(defn- regex-body
  [pattern]
  (subs pattern
        (count regex-delimiter)
        (- (count pattern) (count regex-delimiter))))

(defn- case-sensitive?
  [case-mode default]
  (case case-mode
    :sensitive   true
    :insensitive false
    default))

(defn compile-pattern
  "Compiles the `--match` argument once, before any line is read.

  - pattern: plain text, or a regex wrapped in slashes such as /status=(\\d+)/
  - case-mode: :sensitive, :insensitive, or nil for the per-kind default
    (literals ignore case, regexes respect it)

  Returns a function from line content to a map of named groups on a match
  ({} when there are none), or nil. Returns error data for an invalid regex."
  [pattern case-mode]
  (if (regex-pattern? pattern)
    (regex/compile-regex (regex-body pattern)
                         (case-sensitive? case-mode
                                          regex-case-sensitive-by-default?))
    (literal/compile-literal pattern
                             (case-sensitive?
                              case-mode
                              literal-case-sensitive-by-default?))))
