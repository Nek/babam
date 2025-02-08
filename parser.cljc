#!/usr/bin/env bb

 (ns parser
   (:require [clojure.string :as s]
             [babashka.pods :as pods]))

;; (pods/load-pod 'com.github.jackdbd/pod.jackdbd.jsoup "0.3.0")

;; (require '[pod.jackdbd.jsoup :as jsoup])

 (let [[input] *command-line-args*]
   (let [content (slurp input)
         [cljc html] (s/split content #"(?m)^---$")
         ns (symbol (first (s/split cljc #"\.")))]
     (in-ns ns)
     (eval (read-string cljc))
     (println (s/replace html #"\{\{([^\}]+)\}\}" (fn [element]
                                                    (let [binding (second element)
                                                          value (eval (read-string binding))]
                                                      value))))))
