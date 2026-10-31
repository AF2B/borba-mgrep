(ns borba.mgrep.cli.args-test
  (:require
   [borba.mgrep.cli.args :as args]
   [borba.mgrep.shared.errors :as errors]
   [clojure.test :refer [deftest is testing]]))

(def ^:private defaults
  {:source nil :pattern nil :case-mode nil :where [] :emit nil
   :count-by nil})

(defn- error-type
  [result]
  (get-in result [:error :type]))

(deftest parse-args-reads-source-and-match
  (is (= (merge defaults {:source "app.log" :pattern "ERROR"})
         (args/parse-args ["app.log" "--match" "ERROR"])))
  (is (= (merge defaults {:source "app.log" :pattern "ERROR"})
         (args/parse-args ["--match" "ERROR" "app.log"]))))

(deftest parse-args-defaults-to-stdin
  (is (= (merge defaults {:source nil :pattern "ERROR"})
         (args/parse-args ["--match" "ERROR"])))
  (is (= (merge defaults {:source "-" :pattern "ERROR"})
         (args/parse-args ["-" "--match" "ERROR"]))))

(deftest parse-args-accepts-short-options-and-inline-values
  (is (= (merge defaults {:source nil :pattern "ERROR" :where ["a > 1"]})
         (args/parse-args ["-m" "ERROR" "-w" "a > 1"])))
  (is (= (merge defaults {:source nil :pattern "ERROR" :case-mode :sensitive})
         (args/parse-args ["--match=ERROR" "--case=sensitive"]))))

(deftest parse-args-collects-repeated-where-clauses-in-order
  (is (= ["line.length > 1" "line.contains(\"x\")"]
         (:where (args/parse-args ["--match" "E"
                                   "--where" "line.length > 1"
                                   "--where" "line.contains(\"x\")"])))))

(deftest parse-args-reads-the-output-format
  (is (= :json
         (:emit (args/parse-args ["--match" "E" "--emit" "json"]))))
  (is (= :text
         (:emit (args/parse-args ["--match" "E" "--emit=text"])))))

(deftest parse-args-reads-the-count-by-group
  (is (= "code"
         (:count-by (args/parse-args ["--match" "E" "--count-by" "code"]))))
  (is (= "code"
         (:count-by (args/parse-args ["--match" "E" "--count-by=code"])))))

(deftest parse-args-reads-the-case-mode
  (is (= :sensitive
         (:case-mode (args/parse-args ["--match" "e" "--case" "sensitive"]))))
  (is (= :insensitive
         (:case-mode
          (args/parse-args ["--match" "e" "--case" "insensitive"])))))

(deftest parse-args-treats-everything-after-double-dash-as-the-source
  (is (= (merge defaults {:source "-weird.log" :pattern "E"})
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
           ["count-by without value" ["--match" "E" "--count-by"]
            :args/missing-value]
           ["invalid case value" ["--match" "E" "--case" "x"]
            :args/invalid-value]
           ["invalid emit value" ["--match" "E" "--emit" "xml"]
            :args/invalid-value]
           ["two sources" ["a.log" "b.log" "--match" "E"]
            :args/unexpected-argument]]]
    (testing description
      (let [result (args/parse-args argv)]
        (is (errors/error? result))
        (is (= expected-type (error-type result)))))))
