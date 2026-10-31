(ns package
  "Assembles the downloadable archive for the current operating system."
  (:require
   [babashka.fs :as fs]
   [babashka.process :as process]
   [borba.mgrep.shared.constants :as constants]
   [build]))

(set! *warn-on-reflection* true)

(def ^:private dist-dir "dist")

(def ^:private architecture "x64")

(def ^:private executable-permissions "rwxr-xr-x")

(def ^:private license-file "LICENSE")

(defn- platform
  []
  (if (fs/windows?) "windows" "linux"))

(defn- packaging-dir
  []
  (if (fs/windows?) "packaging/windows" "packaging/unix"))

(defn- package-name
  []
  (str "borba-mgrep-" constants/version "-" (platform) "-" architecture))

(defn- stage!
  "Copies the executable, the install scripts and the license into staging."
  [staging]
  (fs/create-dirs staging)
  (fs/copy (build/executable-path)
           staging
           {:replace-existing true})
  (doseq [script (fs/list-dir (packaging-dir))]
    (fs/copy script staging {:replace-existing true}))
  (fs/copy license-file staging {:replace-existing true})
  (when-not (fs/windows?)
    (doseq [file (fs/list-dir staging)]
      (when-not (= license-file (str (fs/file-name file)))
        (fs/set-posix-file-permissions file executable-permissions)))))

(defn- archive!
  "Archives the staged folder and returns the archive path."
  [archive-name]
  (if (fs/windows?)
    (let [archive (str (fs/path dist-dir (str archive-name ".zip")))]
      (fs/zip archive (str (fs/path dist-dir archive-name)) {:root dist-dir})
      archive)
    (let [archive (str (fs/path dist-dir (str archive-name ".tar.gz")))]
      (process/shell "tar"
                     "-czf" archive
                     "-C" dist-dir
                     archive-name)
      archive)))

(defn -main
  "Builds `dist/borba-mgrep-<version>-<platform>-x64` and its archive from the
  executable in `target`."
  []
  (let [archive-name (package-name)
        staging      (str (fs/path dist-dir archive-name))]
    (fs/delete-tree dist-dir)
    (stage! staging)
    (println "Packaged" (archive! archive-name))))
