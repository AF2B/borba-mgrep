(ns borba.mgrep.cli.args-test
  (:require
   [borba.mgrep.cli.args :as args]
   [borba.mgrep.shared.errors :as errors]
   [clojure.test :refer [deftest is testing]]))

(defn- error-type
  [result]
  (get-in result [:error :type]))

(deftest parse-args-reads-source-and-match
  (is (= {:source "app.log" :pattern "ERROR" :case-mode nil :where []}
         (args/parse-args ["app.log" "--match" "ERROR"])))
  (is (= {:source "app.log" :pattern "ERROR" :case-mode nil :where []}
         (args/parse-args ["--match" "ERROR" "app.log"]))))

(deftest parse-args-defaults-to-stdin
  (is (= {:source nil :pattern "ERROR" :case-mode nil :where []}
         (args/parse-args ["--match" "ERROR"])))
  (is (= {:source "-" :pattern "ERROR" :case-mode nil :where []}
         (args/parse-args ["-" "--match" "ERROR"]))))

(deftest parse-args-accepts-short-options-and-inline-values
  (is (= {:source nil :pattern "ERROR" :case-mode nil :where ["a > 1"]}
         (args/parse-args ["-m" "ERROR" "-w" "a > 1"])))
  (is (= {:source nil :pattern "ERROR" :case-mode :sensitive :where []}
         (args/parse-args ["--match=ERROR" "--case=sensitive"]))))

(deftest parse-args-collects-repeated-where-clauses-in-order
  (is (= ["line.length > 1" "line.contains(\"x\")"]
         (:where (args/parse-args ["--match" "E"
                                   "--where" "line.length > 1"
                                   "--where" "line.contains(\"x\")"])))))

(deftest parse-args-reads-the-case-mode
  (is (= :sensitive
         (:case-mode (args/parse-args ["--match" "e" "--case" "sensitive"]))))
  (is (= :insensitive
         (:case-mode (args/parse-args ["--match" "e" "--case" "insensitive"])))))

(deftest parse-args-treats-everything-after-double-dash-as-the-source
  (is (= {:source "-weird.log" :pattern "E" :case-mode nil :where []}
         (args/parse-args ["--match" "E" "--" "-weird.log"]))))

(deftest parse-args-recognizes-help-and-version
  (is (= {:help true} (args/parse-args ["--help"])))
  (is (= {:help true} (args/parse-args ["-h"])))
  (is (= {:version true} (args/parse-args ["--version"])))
  (testing "they win over other problems"
    (is (= {:help true} (args/parse-args ["--bogus" "--help"])))))

(deftest parse-args-reports-usage-errors
  (doseq [[description argv expected-type]
          [["missing match" ["app.log"] :args/missing-pattern]
           ["unknown option" ["--match" "E" "--bogus"] :args/unknown-option]
           ["option without value" ["--match"] :args/missing-value]
           ["invalid case value" ["--match" "E" "--case" "x"]
            :args/invalid-value]
           ["two sources" ["a.log" "b.log" "--match" "E"]
            :args/unexpected-argument]]]
    (testing description
      (let [result (args/parse-args argv)]
        (is (errors/error? result))
        (is (= expected-type (error-type result)))))))
