(ns borba.mgrep.infra.source
  "Input boundary: turns a file or standard input into numbered lines."
  (:require
   [borba.mgrep.shared.constants :as constants]
   [borba.mgrep.shared.errors :as errors]
   [clojure.java.io :as io])
  (:import
   (java.io BufferedReader File IOException InputStreamReader)
   (java.nio.charset StandardCharsets)))

(set! *warn-on-reflection* true)

(defn lines-from-reader
  "Reads a reader lazily as numbered lines.

  - reader: java.io.BufferedReader positioned at the first line

  Returns a lazy seq of {:line/number n :line/content text}."
  [^BufferedReader reader]
  (map-indexed
   (fn [index content]
     {:line/number  (+ constants/first-line-number index)
      :line/content content})
   (line-seq reader)))

(defn- stdin-source?
  [source]
  (or (nil? source) (= source constants/stdin-marker)))

(defn- stdin-reader
  ^BufferedReader []
  (BufferedReader.
   (InputStreamReader. System/in StandardCharsets/UTF_8)))

(defn- read-file
  [^File file source f]
  (try
    (with-open [reader (io/reader file :encoding "UTF-8")]
      (f (lines-from-reader reader)))
    (catch IOException exception
      (errors/error :source/read-failed
                    (str "cannot read " source ": " (.getMessage exception))
                    {:source source}))))

(defn with-lines
  "Opens a source, calls f with its lines and closes the source afterwards.

  - source: file path, or nil / \"-\" for standard input
  - f: function that must fully consume the lazy line seq before returning

  Returns the result of f, or error data when the source cannot be read."
  [source f]
  (if (stdin-source? source)
    (f (lines-from-reader (stdin-reader)))
    (let [file (io/file source)]
      (cond
        (not (.exists file))
        (errors/error :source/not-found
                      (str "no such file: " source)
                      {:source source})

        (.isDirectory file)
        (errors/error :source/is-directory
                      (str "is a directory: " source)
                      {:source source})

        :else
        (read-file file source f)))))
