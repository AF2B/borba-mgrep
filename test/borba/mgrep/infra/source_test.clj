(ns borba.mgrep.infra.source-test
  (:require
   [borba.mgrep.infra.source :as source]
   [borba.mgrep.shared.errors :as errors]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.io ByteArrayInputStream File StringReader)
   (java.nio.charset StandardCharsets)))

(set! *warn-on-reflection* true)

(defn- with-temp-file
  [content f]
  (let [file (File/createTempFile "borba-mgrep" ".log")]
    (try
      (spit file content :encoding "UTF-8")
      (f (.getPath file))
      (finally
        (.delete file)))))

(deftest lines-from-reader-numbers-lines-from-one
  (is (= [{:line/number 1 :line/content "alpha"}
          {:line/number 2 :line/content "beta"}
          {:line/number 3 :line/content ""}]
         (vec (source/lines-from-reader
               (java.io.BufferedReader.
                (StringReader. "alpha\nbeta\n\n")))))))

(deftest with-lines-reads-a-file
  (with-temp-file
    "ERROR one\nINFO two\n"
    (fn [path]
      (is (= [{:line/number 1 :line/content "ERROR one"}
              {:line/number 2 :line/content "INFO two"}]
             (source/with-lines path vec))))))

(deftest with-lines-decodes-utf-8
  (with-temp-file
    "ação: falhou\n"
    (fn [path]
      (is (= ["ação: falhou"]
             (source/with-lines path #(mapv :line/content %)))))))

(deftest with-lines-reports-a-missing-file
  (let [result (source/with-lines "/nonexistent/borba-mgrep.log" vec)]
    (is (errors/error? result))
    (is (= :source/not-found (get-in result [:error :type])))))

(deftest with-lines-reports-a-directory
  (let [result (source/with-lines (System/getProperty "java.io.tmpdir") vec)]
    (is (errors/error? result))
    (is (= :source/is-directory (get-in result [:error :type])))))

(deftest with-lines-reads-standard-input
  (let [original System/in]
    (try
      (System/setIn
       (ByteArrayInputStream.
        (.getBytes "ERROR piped\nINFO piped\n" StandardCharsets/UTF_8)))
      (testing "nil source selects stdin"
        (is (= ["ERROR piped" "INFO piped"]
               (source/with-lines nil #(mapv :line/content %)))))
      (finally
        (System/setIn original)))))

(deftest with-lines-treats-the-marker-as-stdin
  (let [original System/in]
    (try
      (System/setIn
       (ByteArrayInputStream. (.getBytes "marker\n" StandardCharsets/UTF_8)))
      (is (= ["marker"]
             (source/with-lines "-" #(mapv :line/content %))))
      (finally
        (System/setIn original)))))
