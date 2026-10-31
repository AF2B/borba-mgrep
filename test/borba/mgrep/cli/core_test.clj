(ns borba.mgrep.cli.core-test
  (:require
   [borba.mgrep.cli.core :as cli]
   [borba.mgrep.shared.constants :as constants]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.io ByteArrayInputStream File PrintWriter StringWriter)
   (java.nio.charset StandardCharsets)))

(set! *warn-on-reflection* true)

(defn- run
  [argv]
  (let [out-buffer (StringWriter.)
        err-buffer (StringWriter.)
        code       (cli/run argv
                            (PrintWriter. out-buffer)
                            (PrintWriter. err-buffer))]
    {:code code :out (str out-buffer) :err (str err-buffer)}))

(defn- with-log
  [content f]
  (let [file (File/createTempFile "borba-mgrep" ".log")]
    (try
      (spit file content :encoding "UTF-8")
      (f (.getPath file))
      (finally
        (.delete file)))))

(def ^:private sample-log
  (str "INFO boot\n"
       "ERROR status=503 upstream timeout\n"
       "ERROR status=404 not found\n"
       "WARN slow\n"))

(deftest run-prints-matches-and-exits-zero
  (with-log sample-log
    (fn [path]
      (let [{:keys [code out err]} (run [path "--match" "ERROR"])]
        (is (= 0 code))
        (is (= "ERROR status=503 upstream timeout\nERROR status=404 not found\n"
               out))
        (is (= "" err))))))

(deftest run-exits-one-when-nothing-matches
  (with-log sample-log
    (fn [path]
      (let [{:keys [code out]} (run [path "--match" "FATAL"])]
        (is (= 1 code))
        (is (= "" out))))))

(deftest run-applies-where-clauses
  (with-log sample-log
    (fn [path]
      (testing "named group compared as a number"
        (let [{:keys [code out]} (run [path
                                       "--match" "/status=(?<code>\\d+)/"
                                       "--where" "group(\"code\") >= 500"])]
          (is (= 0 code))
          (is (= "ERROR status=503 upstream timeout\n" out))))
      (testing "several clauses must all hold"
        (let [{:keys [out]} (run [path
                                  "--match" "ERROR"
                                  "--where" "line.contains(\"not\")"
                                  "--where" "line.number > 2"])]
          (is (= "ERROR status=404 not found\n" out)))))))

(deftest run-honors-the-case-option
  (with-log sample-log
    (fn [path]
      (is (= 0 (:code (run [path "--match" "error"]))))
      (is (= 1 (:code (run [path "--match" "error" "--case" "sensitive"])))))))

(deftest run-reads-standard-input
  (let [original System/in]
    (try
      (System/setIn
       (ByteArrayInputStream.
        (.getBytes "ERROR piped\nINFO piped\n" StandardCharsets/UTF_8)))
      (let [{:keys [code out]} (run ["--match" "ERROR"])]
        (is (= 0 code))
        (is (= "ERROR piped\n" out)))
      (finally
        (System/setIn original)))))

(deftest run-emits-json-lines-with-the-captured-groups
  (with-log sample-log
    (fn [path]
      (let [{:keys [code out]} (run [path
                                     "--match" "/status=(?<code>\\d+)/"
                                     "--where" "group(\"code\") >= 500"
                                     "--emit" "json"])]
        (is (= 0 code))
        (is (= (str "{\"line\":2,"
                    "\"content\":\"ERROR status=503 upstream timeout\","
                    "\"groups\":{\"code\":\"503\"}}\n")
               out))))))

(deftest run-rejects-an-unknown-emit-format
  (let [{:keys [code err]} (run ["app.log" "--match" "E" "--emit" "xml"])]
    (is (= 2 code))
    (is (str/includes? err "invalid value for --emit: xml"))))

(def ^:private status-log
  (str "INFO boot\n"
       "ERROR status=503 a\n"
       "ERROR status=404 b\n"
       "ERROR status=503 c\n"
       "WARN status=503 d\n"
       "ERROR no code here\n"))

(def ^:private status-pattern "/status=(?<code>\\d+)/")

(deftest run-counts-matches-by-group-value
  (with-log status-log
    (fn [path]
      (let [{:keys [code out]} (run [path
                                     "--match" status-pattern
                                     "--count-by" "code"])]
        (is (= 0 code))
        (is (= "3 503\n1 404\n" out))))))

(deftest run-counts-only-the-matches-that-pass-the-filters
  (with-log status-log
    (fn [path]
      (let [{:keys [out]} (run [path
                                "--match" status-pattern
                                "--where" "line.contains(\"ERROR\")"
                                "--count-by" "code"])]
        (is (= "2 503\n1 404\n" out))))))

(deftest run-counts-in-json
  (with-log status-log
    (fn [path]
      (let [{:keys [out]} (run [path
                                "--match" status-pattern
                                "--count-by" "code"
                                "--emit" "json"])]
        (is (= (str "{\"value\":\"503\",\"count\":3}\n"
                    "{\"value\":\"404\",\"count\":1}\n")
               out))))))

(deftest run-labels-matches-where-the-group-did-not-take-part
  (with-log "x1\nx\n"
    (fn [path]
      (let [{:keys [out]} (run [path
                                "--match" "/x(?<id>\\d+)?/"
                                "--count-by" "id"])]
        (is (= "1 (none)\n1 1\n" out))))))

(deftest run-prints-nothing-and-exits-one-when-no-match-is-counted
  (with-log status-log
    (fn [path]
      (let [{:keys [code out]} (run [path
                                     "--match" status-pattern
                                     "--where" "line.contains(\"FATAL\")"
                                     "--count-by" "code"])]
        (is (= 1 code))
        (is (= "" out))))))

(deftest run-rejects-a-group-the-pattern-does-not-declare
  (testing "regex with other groups"
    (let [{:keys [code out err]} (run ["/nonexistent/x.log"
                                       "--match" status-pattern
                                       "--count-by" "cde"])]
      (is (= 2 code))
      (is (= "" out))
      (is (str/includes?
           err
           "no named group 'cde' in the pattern (available: code)"))))
  (testing "plain text has no groups"
    (let [{:keys [code err]} (run ["/nonexistent/x.log"
                                   "--match" "ERROR"
                                   "--count-by" "code"])]
      (is (= 2 code))
      (is (str/includes? err "(available: none)")))))

(deftest run-reports-a-missing-file
  (let [{:keys [code out err]} (run ["/nonexistent/x.log" "--match" "E"])]
    (is (= 2 code))
    (is (= "" out))
    (is (= "mgrep: no such file: /nonexistent/x.log\n" err))))

(deftest run-reports-an-invalid-regex-before-reading
  (let [{:keys [code err]} (run ["/nonexistent/x.log" "--match" "/(/"])]
    (is (= 2 code))
    (is (str/starts-with? err "mgrep: invalid regex"))))

(deftest run-reports-a-bad-where-clause-before-reading
  (let [{:keys [code err]} (run ["/nonexistent/x.log"
                                 "--match" "E"
                                 "--where" "line.size > 1"])]
    (is (= 2 code))
    (is (str/includes? err "unknown attribute line.size"))))

(deftest run-reports-usage-errors-with-a-hint
  (let [{:keys [code out err]} (run ["app.log"])]
    (is (= 2 code))
    (is (= "" out))
    (is (str/includes? err "mgrep: missing --match PATTERN"))
    (is (str/includes? err "mgrep --help"))))

(deftest run-keeps-matching-when-a-filter-fails-on-some-lines
  (with-log "x1\nx\nx1\n"
    (fn [path]
      (let [{:keys [code out err]} (run [path
                                         "--match" "/x(?<id>\\d+)?/"
                                         "--where" "group(\"id\") == 1"])]
        (is (= 2 code))
        (is (= "x1\nx1\n" out))
        (is (= "mgrep: line 2: no such group: id\n" err))))))

(deftest run-prints-help-and-version
  (let [{:keys [code out]} (run ["--help"])]
    (is (= 0 code))
    (is (str/includes? out "Usage: mgrep")))
  (let [{:keys [code out]} (run ["--version"])]
    (is (= 0 code))
    (is (= (str constants/version "\n") out))))
