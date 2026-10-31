(ns borba.mgrep.business.format.json-test
  (:require
   [borba.mgrep.business.format.json :as json]
   [clojure.test :refer [deftest is testing]]))

(deftest encode-scalars
  (is (= "null" (json/encode nil)))
  (is (= "true" (json/encode true)))
  (is (= "false" (json/encode false)))
  (is (= "42" (json/encode 42)))
  (is (= "-7" (json/encode -7)))
  (is (= "3.5" (json/encode 3.5))))

(deftest encode-plain-strings
  (is (= "\"hello\"" (json/encode "hello")))
  (is (= "\"\"" (json/encode ""))))

(deftest encode-escapes-special-characters
  (testing "quotes and backslashes"
    (is (= "\"say \\\"hi\\\"\"" (json/encode "say \"hi\"")))
    (is (= "\"a\\\\b\"" (json/encode "a\\b"))))
  (testing "named control characters"
    (is (= "\"a\\nb\\tc\\rd\"" (json/encode "a\nb\tc\rd")))
    (is (= "\"\\b\\f\"" (json/encode "\b\f"))))
  (testing "other control characters use unicode escapes"
    (is (= "\"\\u0001\\u001f\"" (json/encode "\u0001\u001f")))))

(deftest encode-keeps-non-ascii-text-as-is
  (is (= "\"ação ✓\"" (json/encode "ação ✓"))))

(deftest encode-collections
  (is (= "[1,\"a\",null]" (json/encode [1 "a" nil])))
  (is (= "[]" (json/encode [])))
  (is (= "{}" (json/encode {}))))

(deftest encode-maps-keeps-the-given-key-order
  (is (= "{\"line\":3,\"content\":\"x\",\"groups\":{\"code\":\"503\"}}"
         (json/encode (array-map "line" 3
                                 "content" "x"
                                 "groups" (array-map "code" "503")))))
  (testing "keyword keys are written by name"
    (is (= "{\"a\":1}" (json/encode {:a 1})))))
