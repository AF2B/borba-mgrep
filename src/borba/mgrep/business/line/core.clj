(ns borba.mgrep.business.line.core
  "What a `--where` expression may read from a line.

  A line is {:line/number n :line/content text}. This namespace is the single
  list of attributes and methods reachable through `line.<name>`."
  (:require
   [clojure.string :as str]))

(def attribute-readers
  "Attributes readable as `line.<name>`, mapped to their reader function."
  {:length  (fn [line] (count (:line/content line)))
   :number  :line/number
   :content :line/content})

(def method-tests
  "Methods callable as `line.<name>(\"text\")`, mapped to a predicate over
  the line content and the text argument."
  {:contains   str/includes?
   :startswith str/starts-with?
   :endswith   str/ends-with?})

(defn read-attribute
  "Reads an attribute from a line.

  - line: {:line/number n :line/content text}
  - attribute: key of `attribute-readers`

  Returns the attribute value."
  [line attribute]
  ((get attribute-readers attribute) line))

(defn test-method
  "Applies a text method to the content of a line.

  - line: {:line/number n :line/content text}
  - method: key of `method-tests`
  - text: argument given to the method

  Returns true or false."
  [line method text]
  ((get method-tests method) (:line/content line) text))
