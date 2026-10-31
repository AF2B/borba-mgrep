(ns borba.mgrep.business.pattern.literal-test
  (:require
   [borba.mgrep.business.pattern.literal :as literal]
   [clojure.test :refer [deftest is testing]]))

(deftest compile-literal-case-insensitive
  (let [matcher (literal/compile-literal "error" false)]
    (testing "matches regardless of case and returns empty groups"
      (is (= {} (matcher "ERROR: disk full")))
      (is (= {} (matcher "an Error happened"))))
    (testing "returns nil when the text is absent"
      (is (nil? (matcher "INFO: all good"))))))

(deftest compile-literal-case-sensitive
  (let [matcher (literal/compile-literal "ERROR" true)]
    (is (= {} (matcher "ERROR: disk full")))
    (is (nil? (matcher "error: disk full")))))

(deftest compile-literal-treats-special-characters-literally
  (let [matcher (literal/compile-literal "[ERROR]" true)]
    (is (= {} (matcher "a [ERROR] b")))
    (is (nil? (matcher "ERROR")))))

(deftest compile-literal-empty-text-matches-every-line
  (let [matcher (literal/compile-literal "" true)]
    (is (= {} (matcher "anything")))
    (is (= {} (matcher "")))))
