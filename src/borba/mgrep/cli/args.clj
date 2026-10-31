(ns borba.mgrep.cli.args
  "Command-line parsing, written by hand to keep the project dependency free."
  (:require
   [borba.mgrep.business.format.core :as format]
   [borba.mgrep.shared.errors :as errors]
   [clojure.string :as str]))

(def ^:private value-options
  "Options that take a value, mapped to the key they fill."
  {"--match" :pattern
   "-m"      :pattern
   "--where" :where
   "-w"      :where
   "--case"  :case-mode
   "--emit"  :emit
   "--count-by" :count-by})

(def ^:private help-flags #{"--help" "-h"})

(def ^:private version-flags #{"--version"})

(def ^:private case-modes
  {"sensitive"   :sensitive
   "insensitive" :insensitive})

(def ^:private end-of-options "--")

(def ^:private inline-value-pattern
  "Splits `--option=value` into the option and its value."
  #"^(--[^=]+)=(.*)$")

(def ^:private initial-state
  {:source nil
   :pattern nil
   :case-mode nil
   :where []
   :emit nil
   :count-by nil})

(defn- option-like?
  [arg]
  (and (str/starts-with? arg "-") (> (count arg) 1)))

(defn- split-inline
  [arg]
  (if-let [[_ option value] (re-matches inline-value-pattern arg)]
    [option value]
    [arg nil]))

(defn- invalid-value
  [option value expected]
  (errors/error :args/invalid-value
                (str "invalid value for " option ": " value
                     " (expected " expected ")")
                {:option option}))

(defn- assign
  "Stores an option value in the parsed state, or returns error data."
  [state option-key option value]
  (case option-key
    :where     (update state :where conj value)
    :emit      (let [emit-format (keyword value)]
                 (if (contains? format/formats emit-format)
                   (assoc state :emit emit-format)
                   (invalid-value option value "text or json")))
    :case-mode (if-let [mode (get case-modes value)]
                 (assoc state :case-mode mode)
                 (invalid-value option value "sensitive or insensitive"))
    (assoc state option-key value)))

(defn- add-positional
  [state arg]
  (if (nil? (:source state))
    (assoc state :source arg)
    (errors/error :args/unexpected-argument
                  (str "unexpected argument: " arg)
                  {:argument arg})))

(defn- read-option
  "Consumes an option and its value.
  Returns [new-state remaining-args], or error data."
  [state arg remaining]
  (let [[option inline-value] (split-inline arg)
        option-key            (get value-options option)
        value                 (or inline-value (first remaining))
        rest-args             (if inline-value remaining (rest remaining))]
    (cond
      (nil? option-key)
      (errors/error :args/unknown-option
                    (str "unknown option: " arg)
                    {:option arg})

      (nil? value)
      (errors/error :args/missing-value
                    (str "missing value for " option)
                    {:option option})

      :else
      (errors/then (assign state option-key option value)
                   (fn [new-state] [new-state rest-args])))))

(defn- finish
  [state]
  (if (nil? (:pattern state))
    (errors/error :args/missing-pattern
                  "missing --match PATTERN")
    state))

(defn- parse-state
  [argv]
  (loop [remaining argv
         state     initial-state
         literal?  false]
    (if (empty? remaining)
      state
      (let [arg  (first remaining)
            more (rest remaining)]
        (cond
          (and (not literal?) (= arg end-of-options))
          (recur more state true)

          (or literal? (not (option-like? arg)))
          (let [next-state (add-positional state arg)]
            (if (errors/error? next-state)
              next-state
              (recur more next-state literal?)))

          :else
          (let [step (read-option state arg more)]
            (if (errors/error? step)
              step
              (recur (second step) (first step) literal?))))))))

(defn parse-args
  "Parses command-line arguments.

  - argv: sequence of argument strings, without the program name

  Returns {:help true} or {:version true} when those flags appear anywhere,
  otherwise {:source s :pattern p :case-mode m :where [w ...] :emit f
  :count-by g}, or error data describing the first usage problem."
  [argv]
  (cond
    (some help-flags argv)    {:help true}
    (some version-flags argv) {:version true}
    :else                     (errors/then (parse-state argv) finish)))
