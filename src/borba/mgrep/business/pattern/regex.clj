(ns borba.mgrep.business.pattern.regex
  "Regular-expression matching with named capture groups."
  (:require
   [borba.mgrep.shared.errors :as errors])
  (:import
   (java.util.regex Matcher Pattern PatternSyntaxException)))

(set! *warn-on-reflection* true)

(def ^:private group-name-pattern
  "Matches the opening of a named group, e.g. `(?<code>`."
  #"\(\?<([A-Za-z][A-Za-z0-9]*)>")

(def ^:private case-insensitive-flags
  (bit-or Pattern/CASE_INSENSITIVE Pattern/UNICODE_CASE))

(def ^:private case-sensitive-flags 0)

(defn group-names
  "Lists the named capture groups declared in a regex source.

  - source: regex source text

  Returns a vector of group names in declaration order."
  [source]
  (mapv second (re-seq group-name-pattern source)))

(defn- extract-groups
  [^Matcher matcher names]
  (reduce
   (fn [groups ^String group-name]
     (if-let [captured (.group matcher group-name)]
       (assoc groups group-name captured)
       groups))
   {}
   names))

(defn compile-regex
  "Builds a matcher for a regular expression.

  - source: regex source text, using Java syntax
  - case-sensitive?: when false, case is ignored

  Returns a function from line content to a map of named groups on a match
  ({} when the regex has none), or nil. Returns error data on bad syntax."
  [source case-sensitive?]
  (try
    (let [flags   (if case-sensitive?
                    case-sensitive-flags
                    case-insensitive-flags)
          pattern (Pattern/compile source (int flags))
          names   (group-names source)]
      (fn [^String content]
        (let [matcher (.matcher pattern content)]
          (when (.find matcher)
            (extract-groups matcher names)))))
    (catch PatternSyntaxException exception
      (errors/error :pattern/invalid-regex
                    (str "invalid regex: " (.getDescription exception))
                    {:pattern source}))))
