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

(defn- render-content
  [context]
  (get-in context [:match/line :line/content]))

(defn- capture
  [results]
  (let [out-buffer (StringWriter.)
        err-buffer (StringWriter.)
        summary    (emit/emit-all! results
                                   render-content
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

(deftest emit-all-prints-what-the-renderer-returns
  (let [out-buffer (StringWriter.)
        _          (emit/emit-all! [(match 1 "a") (match 2 "b")]
                                   #(str "line " (get-in % [:match/line
                                                            :line/number]))
                                   (PrintWriter. out-buffer)
                                   (PrintWriter. (StringWriter.)))]
    (is (= "line 1\nline 2\n" (str out-buffer)))))

(deftest consume-folds-matches-into-a-state-and-reports-failures
  (let [err-buffer (StringWriter.)
        failure    (errors/error :where/eval-error "boom" {:line 2})
        result     (emit/consume! [(match 1 "a") failure (match 3 "b")]
                                  (fn [state context]
                                    (conj state (render-content context)))
                                  []
                                  (PrintWriter. (StringWriter.))
                                  (PrintWriter. err-buffer))]
    (is (= {:state ["a" "b"] :matches 2 :errors 1} result))
    (is (= "mgrep: line 2: boom\n" (str err-buffer)))))

(deftest write-lines-prints-each-line-with-a-newline
  (let [out-buffer (StringWriter.)
        out        (PrintWriter. out-buffer)]
    (emit/write-lines! out ["one" "two"])
    (.flush out)
    (is (= "one\ntwo\n" (str out-buffer)))))
