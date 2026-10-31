(ns borba.mgrep.cli.core
  "Wires arguments, features and I/O together and decides the exit code."
  (:require
   [borba.mgrep.business.pattern.core :as pattern]
   [borba.mgrep.business.pipeline.core :as pipeline]
   [borba.mgrep.business.where.core :as where]
   [borba.mgrep.cli.args :as args]
   [borba.mgrep.cli.help :as help]
   [borba.mgrep.infra.emit :as emit]
   [borba.mgrep.infra.source :as source]
   [borba.mgrep.shared.constants :as constants]
   [borba.mgrep.shared.errors :as errors])
  (:import
   (java.io PrintWriter)))

(set! *warn-on-reflection* true)

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

(defn- search
  [{:keys [source pattern case-mode where]} out err]
  (let [matcher   (pattern/compile-pattern pattern case-mode)
        predicate (where/compile-filters where)]
    (cond
      (errors/error? matcher)
      (fail matcher err)

      (errors/error? predicate)
      (fail predicate err)

      :else
      (let [summary (source/with-lines
                      source
                      (fn [lines]
                        (emit/emit-all! (pipeline/matches lines
                                                          matcher
                                                          predicate)
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
