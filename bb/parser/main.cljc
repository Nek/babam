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
    (c/eval (c/read-string cljc))
    (let [html (s/replace html #"\(\(([^\)]+)\)\)" (fn [[_ code]]
                                                       (let [code (str "(" code ")")
                                                             result (c/eval (c/read-string code))]
                                                         (println "code" code "result" result)
                                                         (str result))))
            parsed (h/parse html)
            hiccup (h/as-hiccup parsed)
            hiccup-zip (z/hiccup-zip hiccup)
            hiccup-str (str (h2/html hiccup))]
        (println html))
      ))
