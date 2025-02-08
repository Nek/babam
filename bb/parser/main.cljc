#!/usr/bin/env bb

 (ns parser.main
   (:require [clojure.string :as s]
             [hickory.core :as h]
             [hickory.zip :as z]
             [clojure.core :as c]
             [hiccup2.core :as h2]
             [clojure.zip :as zip]))
 
(defn -main [input]
  (let [content (c/slurp input)
        [cljc html] (s/split content #"(?m)^---$")
        ns (symbol (first (s/split cljc #"\.")))]
    ;; (println "parsed" (h/as-hiccup parsed))
    (c/in-ns ns)
    
    (let [defs (c/eval (c/read-string cljc))]
      (println "defs" defs)
;;rewrite next regexp to match ((:title env)) the result should be (:title env)
      (let [html (s/replace html #"\(+\(:([^\)]+)\s+[^\)]+\)\)+" (fn [element]
                                                       (let [binding (second element)
                                                             value ((symbol binding) defs)]
                                                         (println "binding" binding "value" value)
                                                         (str value))))
            parsed (h/parse html)
            hiccup (h/as-hiccup parsed)
            hiccup-zip (z/hiccup-zip hiccup)
            hiccup-str (str (h2/html hiccup))])
      (println html))))
