(ns borba.mgrep.cli.core
  "Wires arguments, features and I/O together and decides the exit code."
  (:require
   [borba.mgrep.business.aggregate.core :as aggregate]
   [borba.mgrep.business.format.core :as format]
   [borba.mgrep.business.pattern.core :as pattern]
   [borba.mgrep.business.pipeline.core :as pipeline]
   [borba.mgrep.business.where.core :as where]
   [borba.mgrep.cli.args :as args]
   [borba.mgrep.cli.help :as help]
   [borba.mgrep.infra.emit :as emit]
   [borba.mgrep.infra.source :as source]
   [borba.mgrep.shared.constants :as constants]
   [borba.mgrep.shared.errors :as errors]
   [clojure.string :as str])
  (:import
   (java.io PrintWriter)))

(set! *warn-on-reflection* true)

(def ^:private default-emit-format :text)

(def ^:private usage-hint
  (str "Try '" constants/program-name " --help' for usage."))

(defn- fail
  "Writes error data to err and returns the error exit code."
  [failure ^PrintWriter err]
  (.println err ^String (emit/format-error failure))
  (.flush err)
  constants/exit-error)

(defn- fail-usage
  [failure ^PrintWriter err]
  (let [code (fail failure err)]
    (.println err ^String usage-hint)
    (.flush err)
    code))

(defn- print-text
  [text ^PrintWriter out]
  (.print out ^String text)
  (.flush out)
  constants/exit-matched)

(defn- exit-code
  [{:keys [matches errors]}]
  (cond
    (pos? errors)  constants/exit-error
    (pos? matches) constants/exit-matched
    :else          constants/exit-no-match))

(defn- check-count-group
  "Returns error data when `--count-by` names a group the pattern lacks."
  [pattern count-by]
  (let [available (pattern/declared-groups pattern)]
    (when (and count-by (not (some #{count-by} available)))
      (errors/error :args/unknown-group
                    (str "no named group '" count-by "' in the pattern "
                         "(available: "
                         (if (seq available)
                           (str/join ", " available)
                           "none")
                         ")")
                    {:group count-by}))))

(defn- emit-matches
  [results emit-format out err]
  (emit/emit-all! results
                  (partial format/render-match emit-format)
                  out
                  err))

(defn- emit-counts
  [results count-by emit-format out err]
  (let [{:keys [state matches errors]}
        (emit/consume! results
                       (fn [counts context]
                         (aggregate/tally counts context count-by))
                       {}
                       out
                       err)]
    (emit/write-lines! out
                       (format/render-counts emit-format
                                             (aggregate/ranked state)))
    {:matches matches :errors errors}))

(defn- emit-results
  [results {:keys [emit count-by]} out err]
  (let [emit-format (or emit default-emit-format)]
    (if count-by
      (emit-counts results count-by emit-format out err)
      (emit-matches results emit-format out err))))

(defn- search
  [{:keys [source pattern case-mode where count-by] :as options} out err]
  (let [matcher   (pattern/compile-pattern pattern case-mode)
        predicate (where/compile-filters where)
        group     (check-count-group pattern count-by)]
    (cond
      (errors/error? matcher)
      (fail matcher err)

      (errors/error? predicate)
      (fail predicate err)

      group
      (fail group err)

      :else
      (let [summary (source/with-lines
                      source
                      (fn [lines]
                        (emit-results (pipeline/matches lines
                                                        matcher
                                                        predicate)
                                      options
                                      out
                                      err)))]
        (if (errors/error? summary)
          (fail summary err)
          (exit-code summary))))))

(defn run
  "Runs one mgrep invocation.

  - argv: command-line arguments, without the program name
  - out: java.io.PrintWriter for matches, help and version text
  - err: java.io.PrintWriter for error messages

  Returns the process exit code."
  [argv out err]
  (let [parsed (args/parse-args argv)]
    (cond
      (errors/error? parsed) (fail-usage parsed err)
      (:help parsed)         (print-text help/usage-text out)
      (:version parsed)      (print-text (str constants/version "\n") out)
      :else                  (search parsed out err))))
