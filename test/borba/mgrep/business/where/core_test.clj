(ns borba.mgrep.business.where.core-test
  (:require
   [borba.mgrep.business.where.core :as where]
   [borba.mgrep.shared.errors :as errors]
   [clojure.test :refer [deftest is testing]]))

(def ^:private context
  {:match/line   {:line/number  3
                  :line/content "ERROR status=503 upstream timeout"}
   :match/groups {"code" "503"}})

(deftest no-expressions-accept-every-match
  (let [predicate (where/compile-filters [])]
    (is (true? (predicate context)))))

(deftest single-expression
  (let [predicate (where/compile-filters ["line.contains(\"timeout\")"])]
    (is (true? (predicate context)))
    (is (false? (predicate (assoc-in context
                                     [:match/line :line/content]
                                     "ERROR something else"))))))

(deftest every-expression-must-hold
  (testing "all true"
    (let [predicate (where/compile-filters
                     ["line.length > 10"
                      "line.contains(\"timeout\")"
                      "group(\"code\") >= 500"])]
      (is (true? (predicate context)))))
  (testing "one false"
    (let [predicate (where/compile-filters
                     ["line.length > 10"
                      "group(\"code\") >= 600"])]
      (is (false? (predicate context))))))

(deftest syntax-errors-surface-at-compile-time
  (let [result (where/compile-filters ["line.length >"])]
    (is (errors/error? result))
    (is (= :where/parse-error (get-in result [:error :type])))))

(deftest the-first-bad-expression-is-reported
  (let [result (where/compile-filters ["line.length > 1" "line.size > 1" "@"])]
    (is (errors/error? result))
    (is (re-find #"line\.size" (errors/message result)))))

(deftest evaluation-errors-come-back-from-the-predicate
  (let [predicate (where/compile-filters ["group(\"missing\") == 1"])
        result    (predicate context)]
    (is (errors/error? result))
    (is (= :where/eval-error (get-in result [:error :type])))))

(deftest an-expression-must-evaluate-to-true-or-false
  (let [predicate (where/compile-filters ["line.length"])
        result    (predicate context)]
    (is (errors/error? result))
    (is (= :where/type-error (get-in result [:error :type])))))

(deftest a-failing-expression-stops-the-chain
  (let [predicate (where/compile-filters ["line.length < 1"
                                          "group(\"missing\") == 1"])]
    (is (false? (predicate context)))))
