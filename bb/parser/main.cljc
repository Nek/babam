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
    (c/in-ns ns)
    (let [env (c/eval (c/read-string cljc))]
      (println "env" env)
;;rewrite next regexp to match ((:title env)) the result should be (:title env)
      (let [html (s/replace html #"\(+\(:([^\)]+)\s+[^\)]+\)\)+" (fn [element]
                                                       (let [code (first element)
                                                             _ (println "code" code)
                                                             value (c/eval (c/read-string code))]
                                                         (println element "binding" code "value" value)
                                                         (str value))))
            parsed (h/parse html)
            hiccup (h/as-hiccup parsed)
            hiccup-zip (z/hiccup-zip hiccup)
            hiccup-str (str (h2/html hiccup))])
      (println html))))
