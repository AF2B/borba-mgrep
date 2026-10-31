(ns borba.mgrep.business.pipeline.core
  "Composes matching and filtering over a stream of lines."
  (:require
   [borba.mgrep.shared.errors :as errors]))

(defn- match-line
  "Returns a match context for a line, or nil when the line does not match."
  [matcher line]
  (when-let [groups (matcher (:line/content line))]
    {:match/line line :match/groups groups}))

(defn- apply-predicate
  "Keeps the context when the predicate holds, drops it when it does not and
  turns an evaluation error into error data tagged with the line number."
  [predicate context]
  (let [verdict (predicate context)]
    (cond
      (errors/error? verdict)
      (update verdict
              :error
              assoc
              :line
              (get-in context [:match/line :line/number]))

      verdict
      context)))

(defn matches
  "Lazily selects the lines that match a pattern and pass the filters.

  - lines: seq of {:line/number n :line/content text}
  - matcher: function from line content to a groups map or nil
  - predicate: function from a match context to true, false or error data

  Returns a lazy seq of match contexts {:match/line line :match/groups groups}
  and error data for lines whose filters failed to evaluate."
  [lines matcher predicate]
  (sequence
   (comp (keep #(match-line matcher %))
         (keep #(apply-predicate predicate %)))
   lines))
