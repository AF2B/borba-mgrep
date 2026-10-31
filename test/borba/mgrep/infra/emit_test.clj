(ns borba.mgrep.infra.emit-test
  (:require
   [borba.mgrep.infra.emit :as emit]
   [borba.mgrep.shared.errors :as errors]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.io PrintWriter StringWriter)))

(set! *warn-on-reflection* true)

(defn- match
  [number content]
  {:match/line {:line/number number :line/content content}
   :match/groups {}})

(defn- capture
  [results]
  (let [out-buffer (StringWriter.)
        err-buffer (StringWriter.)
        summary    (emit/emit-all! results
                                   (PrintWriter. out-buffer)
                                   (PrintWriter. err-buffer))]
    {:summary summary
     :out     (str out-buffer)
     :err     (str err-buffer)}))

(deftest emit-all-prints-matching-lines-to-out
  (let [{:keys [summary out err]} (capture [(match 1 "ERROR a")
                                            (match 4 "ERROR b")])]
    (is (= "ERROR a\nERROR b\n" out))
    (is (= "" err))
    (is (= {:matches 2 :errors 0} summary))))

(deftest emit-all-prints-errors-to-err-with-the-line-number
  (let [failure (errors/error :where/eval-error "no such group: id" {:line 2})
        {:keys [summary out err]} (capture [(match 1 "x1")
                                            failure
                                            (match 3 "x3")])]
    (is (= "x1\nx3\n" out))
    (is (= "mgrep: line 2: no such group: id\n" err))
    (is (= {:matches 2 :errors 1} summary))))

(deftest emit-all-prints-errors-without-a-line-number
  (let [{:keys [err]} (capture [(errors/error :where/eval-error "boom")])]
    (is (= "mgrep: boom\n" err))))

(deftest emit-all-handles-an-empty-result
  (testing "reports zero matches and prints nothing"
    (let [{:keys [summary out err]} (capture [])]
      (is (= "" out))
      (is (= "" err))
      (is (= {:matches 0 :errors 0} summary)))))

(deftest format-error-prefixes-the-program-name
  (is (= "mgrep: bad"
         (emit/format-error (errors/error :args/invalid "bad"))))
  (is (= "mgrep: line 9: bad"
         (emit/format-error (errors/error :x/y "bad" {:line 9})))))
