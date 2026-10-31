(ns borba.mgrep.business.format.core
  "Turns results into the text written to standard output."
  (:require
   [borba.mgrep.business.format.json :as json]))

(def formats
  "Names accepted by `--emit`."
  #{:text :json})

(defn- match->json-entry
  [{:match/keys [line groups]}]
  (array-map "line" (:line/number line)
             "content" (:line/content line)
             "groups" (into (sorted-map) groups)))

(defn render-match
  "Renders one match as a single line of output.

  - format-name: :text prints the line as it was read; :json prints an object
    with the line number, its content and the captured groups
  - context: {:match/line line :match/groups {group-name text}}

  Returns the text to print, without a trailing newline."
  [format-name context]
  (case format-name
    :text (get-in context [:match/line :line/content])
    :json (json/encode (match->json-entry context))))
