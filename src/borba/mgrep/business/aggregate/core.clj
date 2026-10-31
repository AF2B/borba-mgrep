(ns borba.mgrep.business.aggregate.core
  "Counts matches by the value of a named regex group.")

(def missing-value
  "Label for matches in which the group did not take part."
  "(none)")

(defn tally
  "Adds one match to the counts.

  - counts: map from group value to how many matches had it
  - context: {:match/line line :match/groups {group-name text}}
  - group-name: the named group to count by

  Returns the updated counts."
  [counts context group-name]
  (let [value (get-in context [:match/groups group-name] missing-value)]
    (update counts value (fnil inc 0))))

(defn ranked
  "Orders counts for display.

  - counts: map from group value to how many matches had it

  Returns a seq of {:value v :count n}, most frequent first and alphabetical
  among equals."
  [counts]
  (->> counts
       (map (fn [[value total]]
              {:value value :count total}))
       (sort-by (juxt (comp - :count) :value))))
