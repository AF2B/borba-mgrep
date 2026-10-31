(ns borba.mgrep.business.where.core
  "Public entry point of the `--where` feature."
  (:require
   [borba.mgrep.business.where.evaluator :as evaluator]
   [borba.mgrep.business.where.lexer :as lexer]
   [borba.mgrep.business.where.parser :as parser]
   [borba.mgrep.shared.errors :as errors]))

(defn- parse-expression
  [expression]
  (parser/parse (lexer/tokenize expression)))

(defn- evaluate-to-boolean
  [ast context]
  (let [value (evaluator/evaluate ast context)]
    (cond
      (errors/error? value) value
      (boolean? value)      value
      :else                 (errors/error
                             :where/type-error
                             "expression must evaluate to true or false"))))

(defn- all-hold
  "Evaluates ASTs in order, stopping at the first false or error."
  [asts context]
  (loop [remaining asts]
    (if (empty? remaining)
      true
      (let [result (evaluate-to-boolean (first remaining) context)]
        (cond
          (errors/error? result) result
          (false? result)        false
          :else                  (recur (rest remaining)))))))

(defn- parse-all
  [expressions]
  (reduce
   (fn [parsed expression]
     (let [ast (parse-expression expression)]
       (if (errors/error? ast)
         (reduced ast)
         (conj parsed ast))))
   []
   expressions))

(defn compile-filters
  "Parses every `--where` clause once, before any line is read.

  - expressions: vector of expression strings; every one must hold

  Returns a predicate from a match context to true, false or error data, or
  error data for the first expression that does not parse."
  [expressions]
  (errors/then
   (parse-all expressions)
   (fn [asts]
     (fn [context]
       (all-hold asts context)))))
