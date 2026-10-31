(ns borba.mgrep.business.pipeline.core-test
  (:require
   [borba.mgrep.business.pattern.core :as pattern]
   [borba.mgrep.business.pipeline.core :as pipeline]
   [borba.mgrep.business.where.core :as where]
   [borba.mgrep.shared.errors :as errors]
   [clojure.test :refer [deftest is testing]]))

(defn- numbered
  [contents]
  (map-indexed (fn [index content]
                 {:line/number (inc index) :line/content content})
               contents))

(defn- contents
  [results]
  (map #(get-in % [:match/line :line/content]) results))

(def ^:private accept-all
  (constantly true))

(deftest matches-yields-matching-lines-in-order
  (let [matcher (pattern/compile-pattern "error" nil)
        lines   (numbered ["ERROR a" "INFO b" "error c"])]
    (is (= ["ERROR a" "error c"]
           (contents (pipeline/matches lines matcher accept-all))))))

(deftest matches-carries-the-captured-groups
  (let [matcher (pattern/compile-pattern "/status=(?<code>\\d+)/" nil)
        lines   (numbered ["status=500" "none"])]
    (is (= [{:match/line   {:line/number 1 :line/content "status=500"}
             :match/groups {"code" "500"}}]
           (vec (pipeline/matches lines matcher accept-all))))))

(deftest matches-applies-the-where-predicate
  (let [matcher   (pattern/compile-pattern "ERROR" nil)
        predicate (where/compile-filters ["line.length > 8"])
        lines     (numbered ["ERROR a" "ERROR longer line"])]
    (is (= ["ERROR longer line"]
           (contents (pipeline/matches lines matcher predicate))))))

(deftest evaluation-errors-are-tagged-with-the-line-and-do-not-stop-the-run
  (let [matcher   (pattern/compile-pattern "/x(?<id>\\d+)?/" nil)
        predicate (where/compile-filters ["group(\"id\") == 1"])
        lines     (numbered ["x1" "x" "x1"])
        results   (vec (pipeline/matches lines matcher predicate))]
    (is (= 3 (count results)))
    (is (not-any? errors/error? [(first results) (last results)]))
    (testing "line without the optional group reports the line number"
      (let [failure (second results)]
        (is (errors/error? failure))
        (is (= 2 (get-in failure [:error :line])))))))

(deftest matches-is-lazy
  (let [matcher (pattern/compile-pattern "ERROR" nil)
        lines   (numbered (cycle ["ERROR x" "INFO y"]))]
    (is (= ["ERROR x" "ERROR x"]
           (contents (take 2 (pipeline/matches lines matcher accept-all)))))))
