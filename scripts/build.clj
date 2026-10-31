(ns build
  "Builds the native executable: AOT-compiles the sources with Clojure, then
  hands the classes to GraalVM native-image."
  (:require
   [babashka.deps :as deps]
   [babashka.fs :as fs]
   [babashka.process :as process]
   [clojure.string :as str]))

(set! *warn-on-reflection* true)

(def ^:private target-dir "target")

(def ^:private classes-dir "target/classes")

(def ^:private main-namespace "borba.mgrep.main")

(def ^:private executable-name "mgrep")

(def ^:private build-time-packages
  "Packages whose classes are initialized while the image is built."
  "clojure,borba")

(def ^:private direct-linking-flag
  "-Dclojure.compiler.direct-linking=true")

(defn executable-path
  "Path of the executable that `-main` produces on this operating system."
  []
  (str (fs/path target-dir
                (str executable-name (when (fs/windows?) ".exe")))))

(defn- project-classpath
  []
  (str/trim (with-out-str (deps/clojure ["-Spath"]))))

(defn- full-classpath
  []
  (str (fs/absolutize classes-dir)
       java.io.File/pathSeparator
       (project-classpath)))

(defn compile-sources!
  "AOT-compiles the project into `target/classes`, starting from a clean
  `target` directory."
  []
  (fs/delete-tree target-dir)
  (fs/create-dirs classes-dir)
  (process/shell "java"
                 "-cp" (full-classpath)
                 direct-linking-flag
                 (str "-Dclojure.compile.path=" classes-dir)
                 "clojure.main"
                 "-e" (str "(compile '" main-namespace ")")))

(defn build-native!
  "Turns the compiled classes into a native executable under `target`."
  []
  (process/shell "native-image"
                 "-cp" (full-classpath)
                 "--no-fallback"
                 (str "--initialize-at-build-time=" build-time-packages)
                 direct-linking-flag
                 "-march=compatibility"
                 "-H:+ReportExceptionStackTraces"
                 (str "-H:Path=" target-dir)
                 (str "-H:Name=" executable-name)
                 main-namespace))

(defn -main
  "Compiles the sources and builds the native executable."
  []
  (when-not (fs/which "native-image")
    (println "native-image not found on PATH; install GraalVM first.")
    (System/exit 1))
  (compile-sources!)
  (build-native!)
  (println "Built" (executable-path)))
