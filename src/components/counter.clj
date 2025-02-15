(ns components.counter)

(defn main [_ attrs & children]
  (let [tag-children (filter coll? children)
        num (count tag-children)]
    [:div attrs (str "Count: " num)]))
