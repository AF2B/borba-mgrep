(ns borba.mgrep.business.format.json
  "Minimal JSON encoder, so output formatting needs no dependency.

  Maps are written in the iteration order of the map given, which lets
  callers choose the key order with `array-map` or `sorted-map`.")

(def ^:private named-escapes
  {\"        "\\\""
   \\        "\\\\"
   \newline  "\\n"
   \return   "\\r"
   \tab      "\\t"
   \backspace "\\b"
   \formfeed "\\f"})

(def ^:private first-printable-code
  "Characters below this code point must always be escaped in JSON."
  0x20)

(def ^:private escapes
  (into named-escapes
        (for [code (range first-printable-code)
              :let [character (char code)]
              :when (not (contains? named-escapes character))]
          [character (format "\\u%04x" code)])))

(defn- encode-string
  [text]
  (str "\"" (apply str (map #(get escapes % %) text)) "\""))

(defn- key-name
  [map-key]
  (if (keyword? map-key)
    (name map-key)
    (str map-key)))

(defn- encode-entry
  [encode [map-key value]]
  (str (encode-string (key-name map-key)) ":" (encode value)))

(defn- join
  [separator parts]
  (apply str (interpose separator parts)))

(defn encode
  "Encodes a value as a JSON string.

  - value: nil, boolean, number, string, keyword, map or sequential collection

  Returns the JSON text, with no whitespace between tokens."
  [value]
  (cond
    (nil? value)        "null"
    (boolean? value)    (str value)
    (number? value)     (str value)
    (string? value)     (encode-string value)
    (keyword? value)    (encode-string (name value))
    (map? value)        (str "{"
                             (join "," (map #(encode-entry encode %) value))
                             "}")
    (sequential? value) (str "[" (join "," (map encode value)) "]")
    :else               (encode-string (str value))))
