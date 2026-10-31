(ns borba.mgrep.business.format.core-test
  (:require
   [borba.mgrep.business.format.core :as format]
   [clojure.test :refer [deftest is testing]]))

(def ^:private context
  {:match/line   {:line/number  3
                  :line/content "ERROR status=503 upstream timeout"}
   :match/groups {"code" "503"}})

(deftest formats-lists-the-supported-names
  (is (= #{:text :json} format/formats)))

(deftest render-match-text-is-the-line-content
  (is (= "ERROR status=503 upstream timeout"
         (format/render-match :text context))))

(deftest render-match-json-has-line-content-and-groups
  (is (= (str "{\"line\":3,"
              "\"content\":\"ERROR status=503 upstream timeout\","
              "\"groups\":{\"code\":\"503\"}}")
         (format/render-match :json context))))

(deftest render-match-json-sorts-groups-by-name
  (is (= "\"groups\":{\"a\":\"1\",\"b\":\"2\"}}"
         (re-find #"\"groups\".*$"
                  (format/render-match
                   :json
                   (assoc context :match/groups {"b" "2" "a" "1"}))))))

(deftest render-match-json-writes-empty-groups-as-an-object
  (is (= "\"groups\":{}}"
         (re-find #"\"groups\".*$"
                  (format/render-match :json
                                       (assoc context :match/groups {}))))))

(deftest render-match-json-escapes-the-content
  (testing "quotes inside the line stay valid JSON"
    (is (= "\"content\":\"say \\\"hi\\\"\""
           (re-find #"\"content\":\"[^,]*"
                    (format/render-match
                     :json
                     (assoc-in context
                               [:match/line :line/content]
                               "say \"hi\"")))))))

(def ^:private entries
  [{:value "503" :count 12}
   {:value "404" :count 3}])

(deftest render-counts-text-aligns-the-counts
  (is (= ["12 503"
          " 3 404"]
         (format/render-counts :text entries))))

(deftest render-counts-json-writes-one-object-per-value
  (is (= ["{\"value\":\"503\",\"count\":12}"
          "{\"value\":\"404\",\"count\":3}"]
         (format/render-counts :json entries))))

(deftest render-counts-of-nothing-is-empty
  (is (= [] (vec (format/render-counts :text []))))
  (is (= [] (vec (format/render-counts :json [])))))
