(ns borba.mgrep.shared.errors
  "Expected failures are plain data: a map with a single `:error` key.

  Successful values never carry an `:error` key, so any function can return
  either its value or an error map and callers branch with `error?`.")

(defn error
  "Builds error data.

  - error-type: namespaced keyword that classifies the failure
  - message: human readable description
  - data: optional map merged into the error for extra context

  Returns {:error {:type error-type :message message ...data}}."
  ([error-type message]
   (error error-type message {}))
  ([error-type message data]
   {:error (merge data {:type error-type :message message})}))

(defn error?
  "Tells whether a value is error data.

  - value: any value

  Returns true only for maps that contain the `:error` key."
  [value]
  (and (map? value) (contains? value :error)))

(defn message
  "Extracts the message from error data.

  - failure: error data built by `error`

  Returns the message string."
  [failure]
  (get-in failure [:error :message]))

(defn then
  "Continues a pipeline only when the previous step succeeded.

  - result: a value or error data
  - f: function applied to the value when it is not an error

  Returns the error data untouched, or the result of calling f."
  [result f]
  (if (error? result)
    result
    (f result)))
