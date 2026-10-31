(ns borba.mgrep.business.pattern.regex-test
  (:require
   [borba.mgrep.business.pattern.regex :as regex]
   [borba.mgrep.shared.errors :as errors]
   [clojure.test :refer [deftest is testing]]))

(deftest group-names-lists-named-groups-in-order
  (is (= ["year" "month"]
         (regex/group-names "(?<year>\\d+)-(?<month>\\d+)")))
  (testing "ignores plain groups and lookbehind"
    (is (= [] (regex/group-names "(a)(?:b)(?<=c)(?<!d)")))))

(deftest compile-regex-matches-anywhere-in-the-line
  (let [matcher (regex/compile-regex "ERR(OR|WARN)" true)]
    (is (= {} (matcher "an ERROR here")))
    (is (= {} (matcher "ERRWARN")))
    (is (nil? (matcher "error here")))))

(deftest compile-regex-case-insensitive
  (let [matcher (regex/compile-regex "error" false)]
    (is (= {} (matcher "An ERROR here")))))

(deftest compile-regex-extracts-named-groups
  (let [matcher (regex/compile-regex "status=(?<code>\\d+)" true)]
    (is (= {"code" "500"} (matcher "GET /x status=500 ok")))
    (is (nil? (matcher "GET /x status=none")))))

(deftest compile-regex-omits-groups-that-did-not-participate
  (let [matcher (regex/compile-regex "a(?<opt>b)?c" true)]
    (is (= {"opt" "b"} (matcher "abc")))
    (is (= {} (matcher "ac")))))

(deftest compile-regex-reports-invalid-syntax
  (let [result (regex/compile-regex "(" true)]
    (is (errors/error? result))
    (is (= :pattern/invalid-regex (get-in result [:error :type])))))
