(ns borba.mgrep.business.where.lexer
  "Splits a `--where` expression into tokens.

  A token is {:type t :value v :position p}, where :position is the zero-based
  offset of the token in the source text."
  (:require
   [borba.mgrep.shared.errors :as errors]
   [clojure.string :as str]))

(def ^:private token-rules
  "Ordered [token-type anchored-regex] pairs; the first rule that matches at
  the current position wins."
  [[:whitespace #"^\s+"]
   [:number     #"^-?\d+(?:\.\d+)?"]
   [:string     #"^\"(?:[^\"\\]|\\.)*\"|^'(?:[^'\\]|\\.)*'"]
   [:operator   #"^(?:>=|<=|==|!=|>|<)"]
   [:ident      #"^[A-Za-z_][A-Za-z0-9_]*"]
   [:lparen     #"^\("]
   [:rparen     #"^\)"]
   [:dot        #"^\."]])

(def ^:private quote-characters #{\" \'})

(def ^:private escaped-character-pattern
  "A backslash followed by a quote or another backslash."
  #"\\([\"'\\])")

(defn- match-rule
  [remaining]
  (some (fn [[token-type pattern]]
          (when-let [text (re-find pattern remaining)]
            [token-type text]))
        token-rules))

(defn- string-value
  [text]
  (-> text
      (subs 1 (dec (count text)))
      (str/replace escaped-character-pattern "$1")))

(defn- number-value
  [text]
  (if (str/includes? text ".")
    (parse-double text)
    (parse-long text)))

(defn- token-value
  [token-type text]
  (case token-type
    :number   (number-value text)
    :string   (string-value text)
    :operator (keyword text)
    text))

(defn- unmatched-error
  [remaining position]
  (if (contains? quote-characters (first remaining))
    (errors/error :where/parse-error
                  (str "unterminated string at position " position)
                  {:position position})
    (errors/error :where/parse-error
                  (str "unexpected character " (pr-str (str (first remaining)))
                       " at position " position)
                  {:position position})))

(defn tokenize
  "Splits an expression into tokens, dropping whitespace.

  - expression: the text of one `--where` clause

  Returns a vector of tokens, or error data at the first bad character."
  [expression]
  (loop [position 0
         tokens   []]
    (if (>= position (count expression))
      tokens
      (let [remaining       (subs expression position)
            [token-type text] (match-rule remaining)
            value           (when token-type (token-value token-type text))]
        (cond
          (nil? token-type)
          (unmatched-error remaining position)

          (= :whitespace token-type)
          (recur (+ position (count text)) tokens)

          (nil? value)
          (errors/error :where/parse-error
                        (str "number out of range at position " position)
                        {:position position})

          :else
          (recur (+ position (count text))
                 (conj tokens {:type     token-type
                               :value    value
                               :position position})))))))
