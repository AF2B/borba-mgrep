(ns borba.mgrep.test-runner
  "Zero-dependency test runner shared by `bb test` and `clojure -X:test`."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :as test]))

(set! *warn-on-reflection* true)

(def ^:private test-root "test")

(def ^:private test-file-suffix "_test.clj")

(def ^:private clojure-file-suffix ".clj")

(def ^:private path-separator-pattern #"[\\/]")

(def ^:private exit-ok 0)

(def ^:private exit-failed 1)

(defn- file->namespace
  "Converts a test file under `test/` into its namespace symbol."
  [^java.io.File file]
  (let [root-file ^java.io.File (io/file test-root)
        root-path ^java.nio.file.Path (.toPath root-file)
        relative  (str (.relativize root-path (.toPath file)))
        stem      (subs relative
                        0
                        (- (count relative) (count clojure-file-suffix)))]
    (-> stem
        (str/replace path-separator-pattern ".")
        (str/replace "_" "-")
        symbol)))

(defn discover-namespaces
  "Returns the sorted test namespaces found under `test/`."
  []
  (->> (file-seq (io/file test-root))
       (filter #(str/ends-with? (.getName ^java.io.File %) test-file-suffix))
       (map file->namespace)
       (sort-by str)))

(defn run
  "Runs every `*_test.clj` under `test/` and exits non-zero on failure.

  - options: ignored; accepted so the function works with `clojure -X`"
  [_options]
  (let [namespaces (discover-namespaces)]
    (when (seq namespaces)
      (apply require namespaces))
    (let [{:keys [fail error]} (if (seq namespaces)
                                 (apply test/run-tests namespaces)
                                 {:fail 0 :error 0})]
      (System/exit (if (zero? (+ fail error)) exit-ok exit-failed)))))
