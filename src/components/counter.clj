(ns components.counter)

(defn main [attrs & children]
  (println "attrs" attrs)
  (println "children" children)
  (let [num (count children)]
    [:div attrs (str "Count: " num)]))
