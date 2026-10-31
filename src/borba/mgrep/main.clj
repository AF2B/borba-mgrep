(ns borba.mgrep.main
  "Process entry point."
  (:gen-class)
  (:require
   [borba.mgrep.cli.core :as cli])
  (:import
   (java.io BufferedWriter OutputStreamWriter PrintWriter)
   (java.nio.charset StandardCharsets)))

(set! *warn-on-reflection* true)

(defn- stdout-writer
  ^PrintWriter []
  (PrintWriter.
   (BufferedWriter.
    (OutputStreamWriter. System/out StandardCharsets/UTF_8))))

(defn- stderr-writer
  ^PrintWriter []
  (PrintWriter.
   (OutputStreamWriter. System/err StandardCharsets/UTF_8)
   true))

(defn -main
  "Runs mgrep with the given command-line arguments and exits the process.

  - args: command-line arguments"
  [& args]
  (let [out  (stdout-writer)
        err  (stderr-writer)
        code (cli/run (vec args) out err)]
    (.flush out)
    (.flush err)
    (System/exit (int code))))
