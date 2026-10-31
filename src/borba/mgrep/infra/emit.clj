(ns borba.mgrep.infra.emit
  "Output boundary: writes matches to stdout and failures to stderr."
  (:require
   [borba.mgrep.shared.constants :as constants]
   [borba.mgrep.shared.errors :as errors])
  (:import
   (java.io PrintWriter)))

(set! *warn-on-reflection* true)

(def ^:private newline-text "\n")

(def ^:private error-check-interval
  "Matches written between checks for a closed output. Checking flushes the
  writer, so doing it on every line would defeat buffering."
  1024)

(defn format-error
  "Formats error data as one line for stderr.

  - failure: error data, optionally carrying the :line it happened on

  Returns the text prefixed with the program name."
  [failure]
  (let [line-number (get-in failure [:error :line])]
    (str constants/program-name ": "
         (when line-number (str "line " line-number ": "))
         (errors/message failure))))

(defn write-lines!
  "Writes each line followed by a newline.

  - out: java.io.PrintWriter
  - lines: seq of strings to print"
  [^PrintWriter out lines]
  (doseq [line lines]
    (.write out ^String line)
    (.write out ^String newline-text)))

(defn- emit-failure!
  [^PrintWriter out ^PrintWriter err result]
  (.flush out)
  (write-lines! err [(format-error result)])
  (.flush err))

(defn- output-failed?
  [^PrintWriter out matches]
  (and (pos? matches)
       (zero? (mod matches error-check-interval))
       (.checkError out)))

(defn consume!
  "Walks the results once, reporting failures and folding matches into a state.

  Stops early when out can no longer be written, for example when the reader
  of a pipe such as `| head` has gone away.

  - results: seq of match contexts and error data, consumed once
  - step: function of the state and a match context that returns the new state
  - initial-state: state passed to the first step
  - out: java.io.PrintWriter checked for a closed pipe
  - err: java.io.PrintWriter for failure messages

  Returns {:state final-state :matches n :errors n}."
  [results step initial-state ^PrintWriter out ^PrintWriter err]
  (loop [remaining results
         state     initial-state
         matches   0
         failures  0]
    (cond
      (or (empty? remaining) (output-failed? out matches))
      (do (.flush out)
          {:state state :matches matches :errors failures})

      (errors/error? (first remaining))
      (do (emit-failure! out err (first remaining))
          (recur (rest remaining) state matches (inc failures)))

      :else
      (recur (rest remaining)
             (step state (first remaining))
             (inc matches)
             failures))))

(defn emit-all!
  "Writes one rendered line per match to out and each failure to err.

  - results: seq of match contexts and error data, consumed once
  - render: function from a match context to the text of its line
  - out: java.io.PrintWriter for matched lines
  - err: java.io.PrintWriter for failure messages

  Returns {:matches n :errors n}."
  [results render ^PrintWriter out ^PrintWriter err]
  (-> (consume! results
                (fn [_ context]
                  (write-lines! out [(render context)]))
                nil
                out
                err)
      (select-keys [:matches :errors])))
