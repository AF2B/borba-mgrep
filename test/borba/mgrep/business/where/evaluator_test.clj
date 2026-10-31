(ns borba.mgrep.business.where.evaluator-test
  (:require
   [borba.mgrep.business.where.evaluator :as evaluator]
   [borba.mgrep.shared.errors :as errors]
   [clojure.test :refer [deftest is testing]]))

(def ^:private context
  {:match/line   {:line/number  7
                  :line/content "GET /pay status=500 name=abc"}
   :match/groups {"code" "500" "name" "abc"}})

(defn- error-type
  [result]
  (get-in result [:error :type]))

(deftest evaluate-literals
  (is (= 5 (evaluator/evaluate [:literal 5] context)))
  (is (= "x" (evaluator/evaluate [:literal "x"] context)))
  (is (true? (evaluator/evaluate [:literal true] context))))

(deftest evaluate-line-attributes-and-methods
  (is (= 7 (evaluator/evaluate [:attr :number] context)))
  (is (= 28 (evaluator/evaluate [:attr :length] context)))
  (is (true? (evaluator/evaluate [:call :contains "pay"] context)))
  (is (false? (evaluator/evaluate [:call :startswith "POST"] context))))

(deftest evaluate-group-lookup
  (is (= "500" (evaluator/evaluate [:group "code"] context)))
  (testing "a group the pattern did not capture is an evaluation error"
    (let [result (evaluator/evaluate [:group "missing"] context)]
      (is (errors/error? result))
      (is (= :where/eval-error (error-type result))))))

(deftest compare-numbers
  (is (true? (evaluator/evaluate
              [:compare :> [:attr :length] [:literal 10]]
              context)))
  (is (false? (evaluator/evaluate
               [:compare :< [:attr :number] [:literal 7]]
               context)))
  (is (true? (evaluator/evaluate
              [:compare :>= [:attr :number] [:literal 7]]
              context)))
  (is (true? (evaluator/evaluate
              [:compare :<= [:attr :number] [:literal 7.0]]
              context)))
  (is (true? (evaluator/evaluate
              [:compare :== [:attr :number] [:literal 7.0]]
              context)))
  (is (true? (evaluator/evaluate
              [:compare :!= [:attr :number] [:literal 8]]
              context))))

(deftest compare-strings
  (is (true? (evaluator/evaluate
              [:compare :== [:group "name"] [:literal "abc"]]
              context)))
  (is (true? (evaluator/evaluate
              [:compare :< [:group "name"] [:literal "abd"]]
              context)))
  (is (true? (evaluator/evaluate
              [:compare :!= [:group "name"] [:literal "xyz"]]
              context))))

(deftest compare-coerces-numeric-strings-to-numbers
  (is (true? (evaluator/evaluate
              [:compare :>= [:group "code"] [:literal 500]]
              context)))
  (is (false? (evaluator/evaluate
               [:compare :> [:group "code"] [:literal 500]]
               context)))
  (is (true? (evaluator/evaluate
              [:compare :== [:literal 500] [:group "code"]]
              context))))

(deftest compare-reports-incompatible-types
  (doseq [[description node]
          [["non-numeric string against a number"
            [:compare :> [:group "name"] [:literal 5]]]
           ["boolean against a number"
            [:compare :== [:literal true] [:literal 1]]]]]
    (testing description
      (let [result (evaluator/evaluate node context)]
        (is (errors/error? result))
        (is (= :where/type-error (error-type result)))))))

(deftest logical-operators
  (let [truthy [:call :contains "pay"]
        falsy  [:call :contains "nope"]]
    (is (true? (evaluator/evaluate [:and truthy truthy] context)))
    (is (false? (evaluator/evaluate [:and truthy falsy] context)))
    (is (true? (evaluator/evaluate [:or falsy truthy] context)))
    (is (false? (evaluator/evaluate [:or falsy falsy] context)))
    (is (false? (evaluator/evaluate [:not truthy] context)))
    (is (true? (evaluator/evaluate [:not falsy] context)))))

(deftest logical-operators-short-circuit
  (let [broken [:group "missing"]]
    (testing "and stops at the first false operand"
      (is (false? (evaluator/evaluate
                   [:and [:literal false] broken]
                   context))))
    (testing "or stops at the first true operand"
      (is (true? (evaluator/evaluate
                  [:or [:literal true] broken]
                  context))))
    (testing "an error is reached when no operand decides first"
      (is (errors/error? (evaluator/evaluate
                          [:and [:literal true] broken]
                          context))))))

(deftest logical-operators-require-booleans
  (doseq [node [[:and [:literal 1] [:literal true]]
                [:or [:literal "x"] [:literal true]]
                [:not [:literal 3]]]]
    (let [result (evaluator/evaluate node context)]
      (is (errors/error? result))
      (is (= :where/type-error (error-type result))))))
