(ns borba.mgrep.shared.errors-test
  (:require
   [borba.mgrep.shared.errors :as errors]
   [clojure.test :refer [deftest is testing]]))

(deftest error-builds-error-data
  (testing "without extra data"
    (is (= {:error {:type    :where/parse-error
                    :message "unexpected token"}}
           (errors/error :where/parse-error "unexpected token"))))
  (testing "with extra data merged into the error map"
    (is (= {:error {:type     :where/parse-error
                    :message  "unexpected token"
                    :position 7}}
           (errors/error :where/parse-error
                         "unexpected token"
                         {:position 7})))))

(deftest error?-detects-error-data
  (is (errors/error? (errors/error :source/not-found "missing")))
  (is (not (errors/error? {:line/number 1 :line/content "text"})))
  (is (not (errors/error? nil)))
  (is (not (errors/error? 42)))
  (is (not (errors/error? "text"))))

(deftest then-continues-only-on-success
  (testing "applies the function to a successful value"
    (is (= 2 (errors/then 1 inc))))
  (testing "short-circuits on error data"
    (let [failure (errors/error :args/invalid "bad")]
      (is (= failure (errors/then failure inc))))))

(deftest message-extracts-the-error-message
  (is (= "bad" (errors/message (errors/error :args/invalid "bad")))))
