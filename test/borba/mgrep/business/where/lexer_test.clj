(ns borba.mgrep.business.where.lexer-test
  (:require
   [borba.mgrep.business.where.lexer :as lexer]
   [borba.mgrep.shared.errors :as errors]
   [clojure.test :refer [deftest is testing]]))

(deftest tokenize-attribute-comparison
  (is (= [{:type :ident :value "line" :position 0}
          {:type :dot :value "." :position 4}
          {:type :ident :value "length" :position 5}
          {:type :operator :value :> :position 12}
          {:type :number :value 120 :position 14}]
         (lexer/tokenize "line.length > 120"))))

(deftest tokenize-method-call-with-a-string
  (is (= [{:type :ident :value "line" :position 0}
          {:type :dot :value "." :position 4}
          {:type :ident :value "contains" :position 5}
          {:type :lparen :value "(" :position 13}
          {:type :string :value "timeout" :position 14}
          {:type :rparen :value ")" :position 23}]
         (lexer/tokenize "line.contains(\"timeout\")"))))

(deftest tokenize-every-operator
  (is (= [:> :< :>= :<= :== :!=]
         (map :value (lexer/tokenize "> < >= <= == !=")))))

(deftest tokenize-numbers
  (is (= [120 -5 3.14]
         (map :value (lexer/tokenize "120 -5 3.14")))))

(deftest tokenize-strings
  (testing "single quotes"
    (is (= ["a b"] (map :value (lexer/tokenize "'a b'")))))
  (testing "escaped quotes and backslashes"
    (is (= ["say \"hi\"" "back\\slash"]
           (map :value (lexer/tokenize "\"say \\\"hi\\\"\" 'back\\\\slash'")))))
  (testing "other escapes are kept as written"
    (is (= ["a\\db"] (map :value (lexer/tokenize "\"a\\db\""))))))

(deftest tokenize-skips-whitespace
  (is (= [] (lexer/tokenize "   \t "))))

(deftest tokenize-reports-unexpected-characters
  (let [result (lexer/tokenize "line.length @ 5")]
    (is (errors/error? result))
    (is (= :where/parse-error (get-in result [:error :type])))
    (is (= 12 (get-in result [:error :position])))))

(deftest tokenize-reports-unterminated-strings
  (let [result (lexer/tokenize "line.contains(\"abc")]
    (is (errors/error? result))
    (is (= 14 (get-in result [:error :position])))))
