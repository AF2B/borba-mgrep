(ns borba.mgrep.business.pattern.core-test
  (:require
   [borba.mgrep.business.pattern.core :as pattern]
   [borba.mgrep.shared.errors :as errors]
   [clojure.test :refer [deftest is testing]]))

(deftest literal-patterns-ignore-case-by-default
  (let [matcher (pattern/compile-pattern "error" nil)]
    (is (= {} (matcher "ERROR: disk full")))
    (is (nil? (matcher "INFO: fine")))))

(deftest regex-patterns-are-wrapped-in-slashes-and-case-sensitive-by-default
  (let [matcher (pattern/compile-pattern "/ERR(OR|WARN)/" nil)]
    (is (= {} (matcher "an ERROR")))
    (is (nil? (matcher "an error")))))

(deftest case-mode-overrides-the-default
  (testing "literal forced to case-sensitive"
    (let [matcher (pattern/compile-pattern "error" :sensitive)]
      (is (nil? (matcher "ERROR")))
      (is (= {} (matcher "error")))))
  (testing "regex forced to case-insensitive"
    (let [matcher (pattern/compile-pattern "/error/" :insensitive)]
      (is (= {} (matcher "ERROR"))))))

(deftest regex-patterns-expose-named-groups
  (let [matcher (pattern/compile-pattern "/status=(?<code>\\d+)/" nil)]
    (is (= {"code" "404"} (matcher "GET status=404")))))

(deftest a-lone-slash-is-a-literal
  (let [matcher (pattern/compile-pattern "/" nil)]
    (is (= {} (matcher "a/b")))
    (is (nil? (matcher "ab")))))

(deftest invalid-regex-is-reported-as-error-data
  (let [result (pattern/compile-pattern "/(/" nil)]
    (is (errors/error? result))
    (is (= :pattern/invalid-regex (get-in result [:error :type])))))
