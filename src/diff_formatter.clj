(ns chicago.diff-formatter)

(defn- extract-contents [[_ var-map diff]]
  (let [vars (map (comp read-string second) (re-seq #"\{\".*?\":(.*?)\}" var-map))]
    [vars (read-string diff)]))

(defn format-diffs [data]
  (let [[_ name content] (re-find #"\[\"(.*?)\",(.*)\]" data)
        content-details (re-seq #"\[\[(?:(.*?)\],\d+,(-?\d))*?\]" content)]
    [name (map extract-contents content-details)]))
