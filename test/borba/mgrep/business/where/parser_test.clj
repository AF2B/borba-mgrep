(ns borba.mgrep.business.where.parser-test
  (:require
   [borba.mgrep.business.where.lexer :as lexer]
   [borba.mgrep.business.where.parser :as parser]
   [borba.mgrep.shared.errors :as errors]
   [clojure.test :refer [deftest is testing]]))

(defn- parse
  [expression]
  (parser/parse (lexer/tokenize expression)))

(deftest parse-attribute-comparison
  (is (= [:compare :> [:attr :length] [:literal 120]]
         (parse "line.length > 120"))))

(deftest parse-method-call
  (is (= [:call :contains "timeout"]
         (parse "line.contains(\"timeout\")"))))

(deftest parse-group-comparison
  (is (= [:compare :>= [:group "code"] [:literal 500]]
         (parse "group(\"code\") >= 500"))))

(deftest parse-literals
  (is (= [:literal true] (parse "true")))
  (is (= [:literal false] (parse "false")))
  (is (= [:literal "text"] (parse "\"text\"")))
  (is (= [:literal 7] (parse "7"))))

(deftest and-binds-tighter-than-or
  (is (= [:or
          [:call :contains "a"]
          [:and [:call :contains "b"] [:call :contains "c"]]]
         (parse (str "line.contains(\"a\") or "
                     "line.contains(\"b\") and line.contains(\"c\")")))))

(deftest chains-are-flattened
  (is (= [:and
          [:call :contains "a"]
          [:call :contains "b"]
          [:call :contains "c"]]
         (parse (str "line.contains(\"a\") and "
                     "line.contains(\"b\") and "
                     "line.contains(\"c\")")))))

(deftest not-applies-to-a-whole-comparison
  (is (= [:not [:compare :> [:attr :length] [:literal 5]]]
         (parse "not line.length > 5")))
  (is (= [:not [:not [:call :contains "x"]]]
         (parse "not not line.contains(\"x\")"))))

(deftest parentheses-group-expressions
  (is (= [:and
          [:or
           [:compare :> [:attr :length] [:literal 5]]
           [:compare :< [:attr :number] [:literal 2]]]
          [:call :contains "a"]]
         (parse (str "(line.length > 5 or line.number < 2) "
                     "and line.contains(\"a\")")))))

(deftest parse-reports-syntax-errors
  (doseq [[description expression]
          [["empty input" ""]
           ["dangling operator" "line.length >"]
           ["trailing token" "line.length > 1 2"]
           ["missing closing parenthesis" "(line.length > 1"]
           ["unknown object" "file.length > 1"]
           ["unknown attribute" "line.size > 1"]
           ["unknown method" "line.matches(\"x\")"]
           ["non-string method argument" "line.contains(5)"]
           ["group without a name" "group() > 1"]
           ["chained comparison" "1 < 2 < 3"]]]
    (testing description
      (let [result (parse expression)]
        (is (errors/error? result))
        (is (= :where/parse-error (get-in result [:error :type])))))))

(deftest parse-propagates-lexer-errors
  (let [result (parser/parse (lexer/tokenize "line.length @ 1"))]
    (is (errors/error? result))
    (is (= :where/parse-error (get-in result [:error :type])))))
