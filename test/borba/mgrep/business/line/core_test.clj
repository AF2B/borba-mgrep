(ns borba.mgrep.business.line.core-test
  (:require
   [borba.mgrep.business.line.core :as line]
   [clojure.test :refer [deftest is testing]]))

(def ^:private sample-line
  {:line/number 7 :line/content "GET /health status=200"})

(deftest read-attribute-exposes-length-number-and-content
  (is (= 22 (line/read-attribute sample-line :length)))
  (is (= 7 (line/read-attribute sample-line :number)))
  (is (= "GET /health status=200"
         (line/read-attribute sample-line :content))))

(deftest test-method-checks-the-line-content
  (testing "contains"
    (is (true? (line/test-method sample-line :contains "health")))
    (is (false? (line/test-method sample-line :contains "timeout"))))
  (testing "startswith"
    (is (true? (line/test-method sample-line :startswith "GET")))
    (is (false? (line/test-method sample-line :startswith "POST"))))
  (testing "endswith"
    (is (true? (line/test-method sample-line :endswith "200")))
    (is (false? (line/test-method sample-line :endswith "500")))))

(deftest registries-list-what-expressions-may-use
  (is (= #{:length :number :content} (set (keys line/attribute-readers))))
  (is (= #{:contains :startswith :endswith} (set (keys line/method-tests)))))
