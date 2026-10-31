(ns borba.mgrep.business.aggregate.core-test
  (:require
   [borba.mgrep.business.aggregate.core :as aggregate]
   [clojure.test :refer [deftest is testing]]))

(defn- match-with
  [groups]
  {:match/line   {:line/number 1 :line/content "text"}
   :match/groups groups})

(deftest tally-counts-each-value-of-the-group
  (let [counts (-> {}
                   (aggregate/tally (match-with {"code" "503"}) "code")
                   (aggregate/tally (match-with {"code" "404"}) "code")
                   (aggregate/tally (match-with {"code" "503"}) "code"))]
    (is (= {"503" 2 "404" 1} counts))))

(deftest tally-ignores-the-other-groups
  (is (= {"503" 1}
         (aggregate/tally {}
                          (match-with {"code" "503" "id" "x"})
                          "code"))))

(deftest tally-counts-matches-without-the-group-under-a-label
  (testing "the group did not take part in the match"
    (is (= {aggregate/missing-value 1}
           (aggregate/tally {} (match-with {}) "code"))))
  (testing "the label is readable"
    (is (= "(none)" aggregate/missing-value))))

(deftest ranked-orders-by-count-then-value
  (is (= [{:value "500" :count 3}
          {:value "503" :count 3}
          {:value "404" :count 1}]
         (aggregate/ranked {"503" 3 "404" 1 "500" 3}))))

(deftest ranked-of-nothing-is-empty
  (is (= [] (vec (aggregate/ranked {})))))
