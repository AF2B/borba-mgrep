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

(defn- write-line!
  [^PrintWriter writer text]
  (.write writer ^String text)
  (.write writer ^String newline-text))

(defn- emit-match!
  [^PrintWriter out result]
  (write-line! out (get-in result [:match/line :line/content])))

(defn- emit-failure!
  [^PrintWriter out ^PrintWriter err result]
  (.flush out)
  (write-line! err (format-error result))
  (.flush err))

(defn- output-failed?
  [^PrintWriter out matches]
  (and (pos? matches)
       (zero? (mod matches error-check-interval))
       (.checkError out)))

(defn emit-all!
  "Consumes results, writing each match to out and each failure to err.

  Stops early when out can no longer be written, for example when the reader
  of a pipe such as `| head` has gone away.

  - results: seq of match contexts and error data, consumed once
  - out: java.io.PrintWriter for matched lines
  - err: java.io.PrintWriter for failure messages

  Returns {:matches n :errors n}."
  [results ^PrintWriter out ^PrintWriter err]
  (loop [remaining results
         matches   0
         failures  0]
    (cond
      (or (empty? remaining) (output-failed? out matches))
      (do (.flush out)
          {:matches matches :errors failures})

      (errors/error? (first remaining))
      (do (emit-failure! out err (first remaining))
          (recur (rest remaining) matches (inc failures)))

      :else
      (do (emit-match! out (first remaining))
          (recur (rest remaining) (inc matches) failures)))))
