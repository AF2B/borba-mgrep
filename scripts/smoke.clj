(ns smoke
  "Runs the built executable through its main behaviors."
  (:require
   [babashka.fs :as fs]
   [babashka.process :as process]
   [borba.mgrep.shared.constants :as constants]
   [build]))

(set! *warn-on-reflection* true)

(def ^:private sample-log
  (str "INFO boot\n"
       "ERROR status=503 upstream timeout\n"
       "ERROR status=404 not found\n"))

(def ^:private missing-file "/nonexistent/borba-mgrep.log")

(defn- cases
  [log-path]
  [{:name "version"
    :args ["--version"]
    :exit constants/exit-matched
    :out  (str constants/version "\n")}

   {:name "literal match ignores case"
    :args [log-path "--match" "error"]
    :exit constants/exit-matched
    :out  (str "ERROR status=503 upstream timeout\n"
               "ERROR status=404 not found\n")}

   {:name "no match"
    :args [log-path "--match" "FATAL"]
    :exit constants/exit-no-match
    :out  ""}

   {:name "where on a named group"
    :args [log-path
           "--match" "/status=(?<code>\\d+)/"
           "--where" "group(\"code\") >= 500"]
    :exit constants/exit-matched
    :out  "ERROR status=503 upstream timeout\n"}

   {:name "standard input"
    :input "ERROR piped\nINFO piped\n"
    :args  ["--match" "ERROR"]
    :exit  constants/exit-matched
    :out   "ERROR piped\n"}

   {:name "missing file"
    :args [missing-file "--match" "E"]
    :exit constants/exit-error
    :out  ""}])

(defn- run-case
  [{:keys [args input]}]
  (apply process/shell
         {:in       (or input "")
          :out      :string
          :err      :string
          :continue true}
         (build/executable-path)
         args))

(defn- failed?
  [{case-name :name :keys [exit out] :as expected}]
  (let [actual (run-case expected)]
    (if (and (= exit (:exit actual)) (= out (:out actual)))
      (do (println "ok  " case-name)
          false)
      (do (println "FAIL" case-name)
          (println "  expected exit" exit "and output" (pr-str out))
          (println "  actual   exit" (:exit actual)
                   "and output" (pr-str (:out actual)))
          (println "  stderr  " (pr-str (:err actual)))
          true))))

(defn -main
  "Runs every smoke case against the native executable and exits non-zero if
  any of them fails."
  []
  (let [log-file (fs/create-temp-file {:suffix ".log"})]
    (spit (str log-file) sample-log)
    (let [failures (count (filter failed? (cases (str log-file))))]
      (fs/delete log-file)
      (System/exit (if (zero? failures) 0 1)))))
