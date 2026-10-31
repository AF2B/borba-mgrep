(ns borba.mgrep.business.where.evaluator
  "Interprets a `--where` AST against one match.

  A match context is {:match/line line :match/groups {group-name text}}.
  Evaluation never throws: failures come back as error data."
  (:require
   [borba.mgrep.business.line.core :as line]
   [borba.mgrep.shared.errors :as errors]))

(declare evaluate)

(def ^:private ordering-tests
  "Maps a comparison operator to a test over the result of `compare`."
  {:>  pos?
   :<  neg?
   :>= (complement neg?)
   :<= (complement pos?)
   :== zero?
   :!= (complement zero?)})

(defn- type-error
  [message]
  (errors/error :where/type-error message))

(defn- describe
  [value]
  (cond
    (number? value)  "number"
    (string? value)  "string"
    (boolean? value) "boolean"
    :else            "value"))

(defn- coerce-to-number
  [text]
  (parse-double text))

(defn- coerce-pair
  "Aligns the types of two operands, turning numeric strings into numbers.
  Returns [left right], or error data when they cannot be compared."
  [left right]
  (cond
    (and (number? left) (number? right))   [left right]
    (and (string? left) (string? right))   [left right]
    (and (boolean? left) (boolean? right)) [left right]

    (and (number? left) (string? right))
    (if-let [number (coerce-to-number right)]
      [left number]
      (type-error (str "cannot compare number with non-numeric string "
                       (pr-str right))))

    (and (string? left) (number? right))
    (if-let [number (coerce-to-number left)]
      [number right]
      (type-error (str "cannot compare non-numeric string "
                       (pr-str left) " with number")))

    :else
    (type-error (str "cannot compare " (describe left)
                     " with " (describe right)))))

(defn- compare-values
  [operator left right]
  (errors/then
   (coerce-pair left right)
   (fn [[aligned-left aligned-right]]
     ((get ordering-tests operator) (compare aligned-left aligned-right)))))

(defn- evaluate-compare
  [operator left-node right-node context]
  (errors/then
   (evaluate left-node context)
   (fn [left]
     (errors/then
      (evaluate right-node context)
      (fn [right]
        (compare-values operator left right))))))

(defn- evaluate-group
  [group-name context]
  (let [groups (:match/groups context)]
    (if (contains? groups group-name)
      (get groups group-name)
      (errors/error :where/eval-error
                    (str "no such group: " group-name)
                    {:group group-name}))))

(defn- require-boolean
  [value]
  (if (boolean? value)
    value
    (type-error (str "expected true or false, got " (describe value)))))

(defn- evaluate-logical
  "Evaluates operands left to right, stopping at the first one equal to the
  operator's deciding value (false for :and, true for :or)."
  [operator operand-nodes context]
  (let [deciding-value (= operator :or)]
    (loop [remaining operand-nodes]
      (if (empty? remaining)
        (not deciding-value)
        (let [value (errors/then (evaluate (first remaining) context)
                                 require-boolean)]
          (cond
            (errors/error? value)       value
            (= value deciding-value)    deciding-value
            :else                       (recur (rest remaining))))))))

(defn evaluate
  "Evaluates an AST node.

  - node: AST node built by the parser
  - context: {:match/line line :match/groups {group-name text}}

  Returns the node's value, or error data on a type or lookup failure."
  [node context]
  (let [[tag & arguments] node]
    (case tag
      :literal (first arguments)
      :attr    (line/read-attribute (:match/line context) (first arguments))
      :call    (line/test-method (:match/line context)
                                 (first arguments)
                                 (second arguments))
      :group   (evaluate-group (first arguments) context)
      :compare (evaluate-compare (first arguments)
                                 (second arguments)
                                 (nth arguments 2)
                                 context)
      :and     (evaluate-logical :and arguments context)
      :or      (evaluate-logical :or arguments context)
      :not     (errors/then (evaluate (first arguments) context)
                            (fn [value]
                              (errors/then (require-boolean value) not))))))
