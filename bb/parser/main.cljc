#!/usr/bin/env bb

 (ns parser.main
   (:require [clojure.string :as s]
             [hickory.core :as h]
             [clojure.core :as c]))
 
(defn -main [input]
  (let [content (c/slurp input)
        [cljc html] (s/split content #"(?m)^---$")
       ;; parsed (h/parse cljc)
        ns (symbol (first (s/split cljc #"\.")))]
    (c/in-ns ns)
    (c/eval (c/read-string cljc))
    (println (s/replace html #"\{\{([^\}]+)\}\}" (fn [element]
                                                   (let [binding (second element)
                                                         value (c/eval (c/read-string binding))]
                                                     value))))))