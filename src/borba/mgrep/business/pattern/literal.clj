(ns borba.mgrep.business.pattern.literal
  "Plain-text matching: the pattern is searched as a substring."
  (:require
   [clojure.string :as str]))

(defn compile-literal
  "Builds a matcher for a plain substring.

  - text: substring to search for; special characters are not interpreted
  - case-sensitive?: when false, case is ignored on both sides

  Returns a function from line content to {} on a match, or nil."
  [text case-sensitive?]
  (if case-sensitive?
    (fn [content]
      (when (str/includes? content text)
        {}))
    (let [needle (str/lower-case text)]
      (fn [content]
        (when (str/includes? (str/lower-case content) needle)
          {})))))
