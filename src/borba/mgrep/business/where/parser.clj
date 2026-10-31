(ns borba.mgrep.business.where.parser
  "Recursive-descent parser that turns tokens into an AST of plain vectors.

  Grammar, from loosest to tightest binding:

    expression := and (\"or\" and)*
    and        := not (\"and\" not)*
    not        := \"not\" not | comparison
    comparison := atom (OPERATOR atom)?
    atom       := NUMBER | STRING | \"true\" | \"false\"
                | \"(\" expression \")\"
                | \"line\" \".\" ATTRIBUTE
                | \"line\" \".\" METHOD \"(\" STRING \")\"
                | \"group\" \"(\" STRING \")\"

  AST nodes: [:literal v], [:attr name], [:call method text], [:group name],
  [:compare operator left right], [:and ...], [:or ...] and [:not node]."
  (:require
   [borba.mgrep.business.line.core :as line]
   [borba.mgrep.shared.errors :as errors]))

(def ^:private keyword-or "or")

(def ^:private keyword-and "and")

(def ^:private keyword-not "not")

(def ^:private keyword-true "true")

(def ^:private keyword-false "false")

(def ^:private keyword-line "line")

(def ^:private keyword-group "group")

(declare parse-or)

(defn- success
  [node tokens]
  {:node node :tokens tokens})

(defn- parse-error
  [message position]
  (errors/error :where/parse-error message {:position position}))

(defn- unexpected-end
  []
  (errors/error :where/parse-error
                "unexpected end of expression"
                {:position :end}))

(defn- unexpected
  [token]
  (parse-error (str "unexpected " (pr-str (:value token))
                    " at position " (:position token))
               (:position token)))

(defn- token-type?
  [token token-type]
  (= token-type (:type token)))

(defn- keyword-token?
  [token text]
  (and (token-type? token :ident) (= text (:value token))))

(defn- expect
  [tokens token-type]
  (let [token (first tokens)]
    (cond
      (nil? token)                     (unexpected-end)
      (token-type? token token-type)   {:token token :tokens (rest tokens)}
      :else                            (unexpected token))))

(defn- expect-sequence
  [tokens token-types]
  (loop [remaining tokens
         expected  token-types
         matched   []]
    (if (empty? expected)
      {:matched matched :tokens remaining}
      (let [step (expect remaining (first expected))]
        (if (errors/error? step)
          step
          (recur (:tokens step)
                 (rest expected)
                 (conj matched (:token step))))))))

(defn- parse-attribute
  [member tokens]
  (let [attribute (keyword (:value member))]
    (if (contains? line/attribute-readers attribute)
      (success [:attr attribute] tokens)
      (parse-error (str "unknown attribute line." (:value member)
                        " at position " (:position member))
                   (:position member)))))

(defn- parse-method-call
  [member tokens]
  (let [method (keyword (:value member))]
    (if (contains? line/method-tests method)
      (errors/then
       (expect-sequence tokens [:lparen :string :rparen])
       (fn [{[_ argument _] :matched remaining :tokens}]
         (success [:call method (:value argument)] remaining)))
      (parse-error (str "unknown method line." (:value member)
                        " at position " (:position member))
                   (:position member)))))

(defn- parse-line-access
  [tokens]
  (errors/then
   (expect-sequence tokens [:ident :dot :ident])
   (fn [{[_ _ member] :matched remaining :tokens}]
     (if (token-type? (first remaining) :lparen)
       (parse-method-call member remaining)
       (parse-attribute member remaining)))))

(defn- parse-group-call
  [tokens]
  (errors/then
   (expect-sequence tokens [:ident :lparen :string :rparen])
   (fn [{[_ _ name-token _] :matched remaining :tokens}]
     (success [:group (:value name-token)] remaining))))

(defn- parse-parenthesized
  [tokens]
  (errors/then
   (expect tokens :lparen)
   (fn [{after-open :tokens}]
     (errors/then
      (parse-or after-open)
      (fn [{:keys [node] after-expression :tokens}]
        (errors/then
         (expect after-expression :rparen)
         (fn [{after-close :tokens}]
           (success node after-close))))))))

(defn- parse-atom
  [tokens]
  (let [token (first tokens)]
    (cond
      (nil? token)
      (unexpected-end)

      (or (token-type? token :number) (token-type? token :string))
      (success [:literal (:value token)] (rest tokens))

      (keyword-token? token keyword-true)
      (success [:literal true] (rest tokens))

      (keyword-token? token keyword-false)
      (success [:literal false] (rest tokens))

      (token-type? token :lparen)
      (parse-parenthesized tokens)

      (keyword-token? token keyword-line)
      (parse-line-access tokens)

      (keyword-token? token keyword-group)
      (parse-group-call tokens)

      :else
      (unexpected token))))

(defn- parse-comparison
  [tokens]
  (errors/then
   (parse-atom tokens)
   (fn [{left :node after-left :tokens}]
     (let [operator (first after-left)]
       (if (token-type? operator :operator)
         (errors/then
          (parse-atom (rest after-left))
          (fn [{right :node after-right :tokens}]
            (success [:compare (:value operator) left right]
                     after-right)))
         (success left after-left))))))

(defn- parse-not
  [tokens]
  (if (keyword-token? (first tokens) keyword-not)
    (errors/then
     (parse-not (rest tokens))
     (fn [{:keys [node] remaining :tokens}]
       (success [:not node] remaining)))
    (parse-comparison tokens)))

(defn- parse-chain
  "Parses OPERAND (KEYWORD OPERAND)* and folds the operands under `tag`."
  [parse-operand separator tag tokens]
  (errors/then
   (parse-operand tokens)
   (fn [{first-node :node after-first :tokens}]
     (loop [nodes     [first-node]
            remaining after-first]
       (if (keyword-token? (first remaining) separator)
         (let [step (parse-operand (rest remaining))]
           (if (errors/error? step)
             step
             (recur (conj nodes (:node step)) (:tokens step))))
         (success (if (= 1 (count nodes))
                    (first nodes)
                    (into [tag] nodes))
                  remaining))))))

(defn- parse-and
  [tokens]
  (parse-chain parse-not keyword-and :and tokens))

(defn- parse-or
  [tokens]
  (parse-chain parse-and keyword-or :or tokens))

(defn parse
  "Parses the tokens of one `--where` clause.

  - tokens: vector produced by the lexer, or error data from the lexer

  Returns the AST, or error data describing the first syntax problem."
  [tokens]
  (cond
    (errors/error? tokens)
    tokens

    (empty? tokens)
    (parse-error "empty expression" 0)

    :else
    (errors/then
     (parse-or tokens)
     (fn [{:keys [node] remaining :tokens}]
       (if (empty? remaining)
         node
         (unexpected (first remaining)))))))
